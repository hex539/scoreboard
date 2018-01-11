package(default_visibility = ["//visibility:public"])
licenses(["restricted"])

java_library(
  name = "lib",
  srcs = glob([
    "proto/src/**/*.java",
  ]),
  deps = [
    "@com_google_protobuf//:protobuf_java",
    "@gson//jar",
    "@guava//jar",
  ],
)
