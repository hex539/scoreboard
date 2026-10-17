load("@protobuf//bazel:proto_library.bzl", "proto_library")
load("@protobuf//bazel:py_proto_library.bzl", "py_proto_library")
load("@protobuf//bazel:java_proto_library.bzl", "java_proto_library")

package(default_visibility = ["//visibility:public"])

py_proto_library(
    name = "python",
    deps = [":proto"],
)

java_proto_library(
    name = "java",
    deps = [":proto"],
)

proto_library(
    name = "proto",
    srcs = [
      "clics_v2020_03.proto",
      "clics_v2026_01.proto",
    ],
    visibility = ["//visibility:private"],
    deps = [
        ":annotations",
        "@protobuf//:descriptor_proto",
        "@protobuf//:duration_proto",
        "@protobuf//:timestamp_proto",
    ],
)

java_proto_library(
    name = "annotations_java",
    deps = [":annotations"],
)

proto_library(
    name = "annotations",
    srcs = ["annotations.proto"],
    visibility = ["//visibility:private"],
    deps = [
        "@protobuf//:descriptor_proto",
    ],
)
