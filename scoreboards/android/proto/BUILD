package(default_visibility = ["//visibility:public"])

java_library(
  name = "java",
  exports = [":java_internal"],
)

java_proto_library(
  name = "java_internal",
  deps = [":proto"],
  visibility = ["//visibility:private"],
)

proto_library(
  name = "proto",
  srcs = ["config.proto"],
  deps = [
    "//contest/proto",
    "@com_google_protobuf//:descriptor_proto",
    "@com_google_protobuf//:duration_proto",
    "@com_google_protobuf//:timestamp_proto",
  ],
  visibility = ["//visibility:private"],
)
