load("@protobuf//bazel:proto_library.bzl", "proto_library")
load("@protobuf//bazel:py_proto_library.bzl", "py_proto_library")
load("@protobuf//bazel:java_proto_library.bzl", "java_proto_library")

package(default_visibility = ["//visibility:public"])

java_proto_library(
    name = "java",
    deps = [":proto"],
)

proto_library(
    name = "proto",
    srcs = ["config.proto"],
    deps = [
        "@protobuf//:descriptor_proto",
        "@protobuf//:duration_proto",
        "@protobuf//:timestamp_proto",
    ],
)
