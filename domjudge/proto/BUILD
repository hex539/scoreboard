load("@org_pubref_rules_protobuf//java:rules.bzl", "java_proto_library")
package(default_visibility = ["//visibility:public"])

java_proto_library(
  name = "domjudge-proto",
  protos = ["src/domjudge.proto"],
  proto_deps = [":annotations-proto"],
  imports = [
    "external/google_protobuf/src",
    "domjudge/proto/src",
  ],
)

java_proto_library(
  name = "annotations-proto",
  protos = ["src/annotations.proto"],
  imports = [
    "external/google_protobuf/src"
  ],
  inputs = [
    "@google_protobuf//:well_known_protos"
  ],
)
