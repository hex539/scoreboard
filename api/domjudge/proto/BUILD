load("@com_google_protobuf//:protobuf.bzl", "py_proto_library")
package(default_visibility = ["//visibility:public"])

py_proto_library(
  name = "python",
  srcs = glob(["*.proto"]),
  deps = [
    "@com_google_protobuf//:protobuf_python",
  ],
  default_runtime="@com_google_protobuf//:protobuf_python",
  protoc="@com_google_protobuf//:protoc",
)

java_proto_library(
  name = "java",
  deps = [":proto"],
)

proto_library(
  name = "proto",
  srcs = ["domjudge_apiv3.proto"],
  deps = [
    ":annotations",
    "@com_google_protobuf//:descriptor_proto",
    "@com_google_protobuf//:duration_proto",
    "@com_google_protobuf//:timestamp_proto",
  ],
  visibility = ["//visibility:private"],
)

java_proto_library(
  name = "annotations_java",
  deps = [":annotations"],
)

proto_library(
  name = "annotations",
  srcs = ["annotations.proto"],
  deps = [
    "@com_google_protobuf//:descriptor_proto",
  ],
  visibility = ["//visibility:private"],
)
