load("@com_google_protobuf//:protobuf.bzl", "py_proto_library")
package(default_visibility = ["//visibility:public"])

py_proto_library(
  name = "python",
  deps = [":proto"],
)

java_proto_library(
  name = "java",
  deps = [":proto"],
#  imports = [
#    "external/com_google_protobuf/src",
#    "clics/proto/src",
#  ],
)

proto_library(
  name = "proto",
  srcs = glob(["src/**"]),
  deps = [
    "@com_google_protobuf//:descriptor_proto",
    "@com_google_protobuf//:duration_proto",
    "@com_google_protobuf//:timestamp_proto",
  ],
  visibility = ["//visibility:private"],
)
