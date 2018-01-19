#!/usr/bin/env python

import sys
import datetime

from clics.proto.src.clics_v1_pb2 import *
from flask import Flask, request, abort
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
path = sys.argv[1]
filter_groups = set(['University of Bath'])
seconds_since_start = 310 * 60
use_text_format = True

clics_orig = ClicsContest()
with open(path, 'rb') as f:
  if use_text_format:
    text_format.Merge(f.read(), clics_orig)
  else:
    clics_orig.ParseFromString(f.read())

clics = ClicsContest()
clics.CopyFrom(clics_orig)
start_time = Timestamp()
start_time.GetCurrentTime()
start_time.seconds -= seconds_since_start
time_diff = (start_time.ToDatetime() - clics_orig.contest.start_time.ToDatetime())

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

def protorepeated_to_array(x):
  return [json_format.MessageToDict(y) for y in x]

def protomap_to_array(x):
  return [json_format.MessageToDict(x[y]) for y in x]

def time_now():
  t = Timestamp()
  t.GetCurrentTime()
  return t

def already_happened(t_now, x):
  for abs_attr in ('time', 'start_time', 'end_time'):
    if hasattr(x, abs_attr):
      if getattr(x, abs_attr).seconds > t_now.seconds:
        return False
  return True

############################

app = Flask(__name__)
api = Api(app)

def trysort(item):
  if hasattr(item, 'ordinal'):
    return int(item.ordinal)
  if hasattr(item, 'start_time'):
    return item.start_time.ToNanoseconds()
  if hasattr(item, 'time'):
    return item.time.ToNanoseconds()
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
            sorted([y for y in l if already_happened(t_now, l[y])], key = trysort)]

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

class Scoreboard(Resource):
  def get(self, cid):
    if clics.contest.id <> cid:
      abort(404)
    else:
      t_now = time_now()
      penalty = dict()

      jugs = [
          json_format.MessageToDict(z) for z in
          sorted([clics.judgements[y] for y in clics.judgements if already_happened(t_now, clics.judgements[y])], key=trysort)]
      probs = [
          json_format.MessageToDict(z) for z in
          sorted([clics.problems[y] for y in clics.problems if already_happened(t_now, clics.problems[y])], key=trysort)]

      allowed_groups = set()
      for g in clics.groups:
        if clics.groups[g].name in filter_groups:
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

      for jug in [x for x in jugs if 'judgement_type_id' in x]:
        sub = clics.submissions[jug['submission_id']]
        typ = clics.judgement_types[jug['judgement_type_id']]
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

        pkey = '%s:%s' % (prob.ordinal, team_id)
        if pkey not in penalty:
          penalty[pkey] = set()

        if typ.penalty or typ.solved:
          attempt.num_judged += 1

        if typ.penalty:
          penalty[pkey].add(sub.id)

        if typ.solved:
          if sub.id in penalty[pkey]:
            penalty[pkey].remove(sub.id)
          attempt.num_judged += 1
          attempt.solved = True
          attempt.time = int(sub.contest_time.seconds / 60)
          row.score.num_solved += 1
          row.score.total_time += attempt.time + len(penalty[pkey]) * clics.contest.penalty_time

      def solved_times(row):
        return list(reversed(sorted([p.time for p in row.problems if p.solved])))

      res = list(sorted(
          team_scoreboards.values(),
          key = lambda x: [-x.score.num_solved, x.score.total_time, solved_times(x)]))

      for i in range(len(res)):
        res[i].rank = i + 1
      return map(json_format.MessageToDict, res)
api.add_resource(Scoreboard, '/contests/<string:cid>/scoreboard/')

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
  app.run()
