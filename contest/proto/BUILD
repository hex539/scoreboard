package(default_visibility = ["//visibility:public"])

java_proto_library(
  name = "java",
  deps = [":proto"],
)

proto_library(
  name = "proto",
  srcs = ["config.proto"],
  deps = [
    "@com_google_protobuf//:descriptor_proto",
    "@com_google_protobuf//:duration_proto",
    "@com_google_protobuf//:timestamp_proto",
  ],
)
