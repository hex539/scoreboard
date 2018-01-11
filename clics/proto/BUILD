load("@org_pubref_rules_protobuf//java:rules.bzl", "java_proto_library")
package(default_visibility = ["//visibility:public"])

java_proto_library(
  name = "proto",
  protos = ["src/clics-v1.proto"],
  proto_deps = [":annotations"],
  imports = [
    "external/com_google_protobuf/src",
    "clics/proto/src",
  ],
)

java_proto_library(
  name = "annotations",
  protos = ["src/annotations.proto"],
  imports = [
    "external/com_google_protobuf/src"
  ],
  inputs = [
    "@com_google_protobuf//:well_known_protos"
  ],
)
