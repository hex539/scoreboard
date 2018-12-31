#!/usr/bin/env python

import datetime
import isodate
import json
import sys
import time

from argparse import ArgumentParser
from api.clics.proto.clics_v1_pb2 import *
from flask import Flask, Response, request, abort, render_template
from flask_restful import Resource, Api
from google.protobuf import text_format, json_format, descriptor
from google.protobuf.timestamp_pb2 import Timestamp

def format_timedelta(d):
  if d.days < 0:
    return '-' + format_timedelta(abs(d))
  else:
    res = str(datetime.timedelta(seconds=d.seconds, microseconds=d.microseconds)).split(':')
    res[0] = str(int(res[0]) + d.days * (60 * 60 * 24))
    return ':'.join(res)

_GenericMessageToJsonObject = json_format._Printer._GenericMessageToJsonObject
def injected_duration_print(self, x):
  if hasattr(x, 'DESCRIPTOR'):
    if x.DESCRIPTOR.full_name == 'google.protobuf.Duration':
      return format_timedelta(datetime.timedelta(seconds=x.seconds, microseconds=x.nanos/1000))
  return _GenericMessageToJsonObject(self, x)
json_format._Printer._GenericMessageToJsonObject = injected_duration_print

############################

# Things that should be configurable:
arg_parser = ArgumentParser(description='Replay a contest from its API snapshot')
arg_parser.add_argument('--path', type=str)
arg_parser.add_argument('--groups', metavar='<group>', type=str, nargs='+', default=[])
arg_parser.add_argument('--offset', type=str, default='PT0H0M0S')
arg_parser.add_argument('--time_accel', type=int, default=1)
arg_parser.add_argument('--textformat', action='store_true')
args = arg_parser.parse_args()

path = args.path
filter_groups = (lambda x: x.name in args.groups or x.id in args.groups) if len(args.groups) > 0 else (lambda x: True)
time_since_start = isodate.parse_duration(args.offset)

clics = ClicsContest()
with open(path, 'rb') as f:
  if args.textformat:
    text_format.Merge(f.read(), clics)
  else:
    clics.ParseFromString(f.read())

beginning_of_time = Timestamp(); beginning_of_time.GetCurrentTime()
time_diff = (beginning_of_time.ToDatetime() - clics.contest.start_time.ToDatetime()) - time_since_start

def time_now():
  t = Timestamp()
  t.GetCurrentTime()
  if args.time_accel <> 1:
    t.FromNanoseconds(0 +
         (10**9) *              beginning_of_time.seconds            + beginning_of_time.nanos +
        ((10**9) * (t.seconds - beginning_of_time.seconds) + t.nanos - beginning_of_time.nanos) * args.time_accel)
  return t

def get_time_of(x):
  for abs_attr in ('time', 'end_time', 'start_time'):
    if hasattr(x, abs_attr):
      return getattr(x, abs_attr)
    elif type(x) is dict and abs_attr in x:
      return x[abs_attr]
  return None

def get_nanos_of(x):
  x = get_time_of(x)
  if x is not None:
    x = x.seconds * 10**9 + x.nanos
  return x

def already_happened(t_now, x):
  t_then = get_time_of(x)
  if t_then:
    return t_then.seconds <= t_now.seconds
  return True

def fix_times(x):
  if hasattr(x, 'DESCRIPTOR') and x.DESCRIPTOR.full_name == 'google.protobuf.Timestamp':
    x.FromDatetime(x.ToDatetime() + time_diff)
    return

  if hasattr(x, 'ListFields'):
    for field, value in x.ListFields():
      if (field.type == descriptor.FieldDescriptor.TYPE_MESSAGE and 
          field.message_type.has_options and 
          field.message_type.GetOptions().map_entry):
        for k in value:
          fix_times(value[k])
      elif field.label == descriptor.FieldDescriptor.LABEL_REPEATED:
        for i in value:
          fix_times(i)
      elif field.type == descriptor.FieldDescriptor.TYPE_MESSAGE:
        fix_times(value)

fix_times(clics)

############################

app = Flask(__name__)
app.url_map.strict_slashes = False

api = Api(app)

def try_sort(item):
  if hasattr(item, 'ordinal'):
    return int(item.ordinal)
  if hasattr(item, 'end_time'):
    return item.end_time.ToNanoseconds()
  if hasattr(item, 'start_time'):
    return item.start_time.ToNanoseconds()
  if hasattr(item, 'time'):
    return item.time.ToNanoseconds()
  if hasattr(item, 'contest_time'):
    return item.contest_time.ToNanoseconds()
  if hasattr(item, 'name'):
    return item.name.upper()
  if hasattr(item, 'id'):
    return (len(item.id), item.id)
  return str(item)

def list_endpoint(name, l):
  class AllOf(Resource):
    def get(self, cid):
      if clics.contest.id <> cid:
        abort(404)
      else:
        t_now = time_now()
        return [
            json_format.MessageToDict(l[z]) for z in
            sorted([y for y in l if already_happened(t_now, l[y])], key = lambda y: try_sort(l[y]))]

  class OneOf(Resource):
    def get(self, cid, iid):
      if clics.contest.id <> cid:
        abort(404)
      else:
        t_now = time_now()
        if not already_happened(t_now, l[iid]):
          abort(404)
        else:
          return json_format.MessageToDict(l[iid])

  AllOf.__name__ += '_' + name
  OneOf.__name__ += '_' + name

  api.add_resource(AllOf, '/contests/<string:cid>/%s/' % (name))
  api.add_resource(OneOf, '/contests/<string:cid>/%s/<string:iid>/' % (name))

############################

def get_problems(t_now):
  return sorted([clics.problems[y] for y in clics.problems
          if already_happened(t_now, clics.problems[y])], key=try_sort)

def get_submissions(t_now):
  return sorted([clics.submissions[y] for y in clics.submissions
          if already_happened(t_now, clics.submissions[y])], key=try_sort)

def get_judgements(t_now):
  return sorted([clics.judgements[y] for y in clics.judgements
          if already_happened(t_now, clics.judgements[y])], key=try_sort)

def get_scoreboard(cid, t_now=None, incl_first=False):
  if clics.contest.id <> cid:
    abort(404)
  else:
    if t_now is None:
      t_now = time_now()
    penalty = dict()
    first_solvers = set()
    problems_solved = set()

    jugs = map(json_format.MessageToDict, get_judgements(t_now))
    subs = map(json_format.MessageToDict, get_submissions(t_now))
    probs = map(json_format.MessageToDict, get_problems(t_now))
    jug_map = dict()
    for jug in [x for x in jugs if 'judgement_type_id' in x]:
      jug_map[jug['submission_id']] = jug['id']

    allowed_groups = set()
    for g in clics.groups:
      if filter_groups(clics.groups[g]):
        allowed_groups.add(g)
    def check_groups(tid):
      for i in clics.teams[tid].group_ids:
        if i in allowed_groups:
          return True
      return False

    team_scoreboards = dict()
    for team_id in sorted(filter(check_groups, clics.teams)):
      row = ScoreboardRow()
      row.team_id = team_id
      row.score.num_solved = 0
      for problem in probs:
        pscore = ScoreboardProblem()
        pscore.problem_id = problem['id']
        row.problems.extend([pscore])
      team_scoreboards[team_id] = row

    for sub in map(lambda s: clics.submissions[s['id']], subs):
      prob = clics.problems[sub.problem_id]
      team_id = sub.team_id

      if sub.contest_time.ToNanoseconds() >= clics.contest.contest_duration.ToNanoseconds():
        continue
      if team_id not in team_scoreboards:
        continue

      row = team_scoreboards[team_id]
      attempt = row.problems[prob.ordinal]
      if attempt.solved:
        continue

      if sub.id not in jug_map:
        attempt.num_pending += 1
        continue

      jug = clics.judgements[jug_map[sub.id]]
      typ = clics.judgement_types[jug.judgement_type_id]

      pkey = '%s:%s' % (prob.ordinal, team_id)
      if pkey not in penalty:
        penalty[pkey] = set()

      if typ.penalty or typ.solved:
        attempt.num_judged += 1

      if typ.penalty:
        penalty[pkey].add(sub.id)

      if typ.solved:
        if sub.id in penalty[pkey]: penalty[pkey].remove(sub.id)
        attempt.solved = True
        attempt.time = int(sub.contest_time.seconds / 60)

        row.score.num_solved += 1
        row.score.total_time += attempt.time + len(penalty[pkey]) * clics.contest.penalty_time

        if not prob.id in problems_solved:
          first_solvers.add('%d:%s' % (prob.ordinal, team_id))
          problems_solved.add(prob.id)

    def solved_times(row):
      return list(reversed(sorted([p.time for p in row.problems if p.solved])))

    res = list(sorted(
        team_scoreboards.values(),
        key = lambda x: [
            -x.score.num_solved,
            +x.score.total_time,
            solved_times(x),
            try_sort(clics.teams[x.team_id])
        ]))

    for i,row in enumerate(res): row.rank = i + 1
    res = map(json_format.MessageToDict, res)
    if incl_first:
      for r in res:
        for i,p in enumerate(r['problems']):
          if 'solved' in p and ('%d:%s' % (i, r['team_id'])) in first_solvers:
            p['solved_first'] = True
    return res

def generate_eventfeed():
  end_time = Timestamp()
  end_time.FromNanoseconds(
      clics.contest.start_time.ToNanoseconds() +
      clics.contest.contest_duration.ToNanoseconds())
  start_evts = []
  evts = []

  start_evts += [{'op': 'create', 'type': 'contests', 'data': clics.contest}]
  for typ in clics.judgement_types.values():
    start_evts += [{'op': 'create', 'type': 'judgement-types', 'data': typ}]
  for prob in get_problems(end_time):
    start_evts += [{'op': 'create', 'type': 'problems', 'data': prob}]
  for group in clics.groups.values():
    start_evts += [{'op': 'create', 'type': 'groups', 'data': group}]
  for group in clics.organizations.values():
    start_evts += [{'op': 'create', 'type': 'organizations', 'data': group}]
  for team in clics.teams.values():
    start_evts += [{'op': 'create', 'type': 'teams', 'data': team}]

  for sub in get_submissions(end_time):
    evts += [{'op': 'create', 'type': 'submissions', 'data': sub}]

  for jug in get_judgements(end_time):
    evts += [{'op': 'create', 'type': 'judgements', 'data': jug}]

  evt_id = 1
  for evt in start_evts + sorted(evts, key=lambda x: get_nanos_of(x['data'])):
    t_now = time_now()
    t_evt = get_time_of(evt['data'])
    if t_evt and t_now.ToNanoseconds() < t_evt.ToNanoseconds():
      time.sleep((t_evt.ToNanoseconds() - t_now.ToNanoseconds()) / (10.0**9) / args.time_accel)
    evt['data'] = json_format.MessageToDict(evt['data'])
    evt['id'] = evt_id
    evt_id += 1
    yield json.dumps(evt) + '\n'

############################

class Contests(Resource):
  def get(self):
    return [json_format.MessageToDict(clics.contest)]
api.add_resource(Contests, '/contests/')

class Contest(Resource):
  def get(self, cid):
    if clics.contest.id <> cid:
      abort(404)
    else:
      return json_format.MessageToDict(clics.contest)
api.add_resource(Contest, '/contests/<string:cid>/')

class State(Resource):
  def get(self, cid):
    if clics.contest.id <> cid:
      abort(404)
    else:
      return json_format.MessageToDict(clics.state)
api.add_resource(State, '/contests/<string:cid>/state/')

class EventFeed(Resource):
  def get(self, cid):
    if clics.contest.id <> cid:
      abort(404)
    else:
      return Response(generate_eventfeed(), mimetype = 'application/json')
api.add_resource(EventFeed, '/contests/<string:cid>/event-feed/')

class Scoreboard(Resource):
  def get(self, cid):
    return get_scoreboard(cid)
api.add_resource(Scoreboard, '/contests/<string:cid>/scoreboard/')

@app.route('/scoreboard/')
def ScoreboardHtml(cid=None):
  if cid == None:
    cid = clics.contest.id
  t_now = time_now()
  return render_template(
      'scoreboard.html',
      scoreboard=get_scoreboard(cid, t_now, True),
      problems=map(json_format.MessageToDict, get_problems(t_now)),
      contest=clics.contest,
      teams=clics.teams)

list_endpoint('judgement-types', clics.judgement_types)
list_endpoint('languages', clics.languages)
list_endpoint('problems', clics.problems)
list_endpoint('groups', clics.groups)
list_endpoint('organizations', clics.organizations)
list_endpoint('teams', clics.teams)
list_endpoint('submissions', clics.submissions)
list_endpoint('judgements', clics.judgements)
list_endpoint('runs', clics.runs)
list_endpoint('clarifications', clics.clarifications)
list_endpoint('awards', clics.awards)

############################

if __name__ == '__main__':
  app.run(threaded = True)
