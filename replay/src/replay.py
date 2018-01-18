#!/usr/bin/env python

import sys
import datetime

from clics.proto.src.clics_v1_pb2 import *
from flask import Flask, request, abort
from flask_restful import Resource, Api
from google.protobuf import text_format, json_format, descriptor

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

path = sys.argv[1]
tf = True

clics = ClicsContest()
with open(path, 'rb') as f:
  if tf:
    text_format.Merge(f.read(), clics)
  else:
    clics.ParseFromString(f.read())

def protorepeated_to_array(x):
  return [json_format.MessageToDict(y) for y in x]

def protomap_to_array(x):
  return [json_format.MessageToDict(x[y]) for y in x]

############################

app = Flask(__name__)
api = Api(app)

def list_endpoint(name, l):
  class AllOf(Resource):
    def get(self, cid):
      if clics.contest.id <> cid:
        abort(404)
      else:
        return protomap_to_array(l)

  class OneOf(Resource):
    def get(self, cid, iid):
      if clics.contest.id <> cid:
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
      return protorepeated_to_array(clics.scoreboard)
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
