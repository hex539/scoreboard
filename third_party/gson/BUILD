package(default_visibility = ["//visibility:public"])
licenses(["restricted"])

java_library(
  name = "lib",
  srcs = glob([
    "proto/src/**/*.java",
  ]),
  deps = [
    "@google_protobuf//:protobuf_java",
    "@gson//jar",
    "@guava//jar",
  ],
)
