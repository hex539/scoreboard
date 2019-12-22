load("@com_google_protobuf//:protobuf.bzl", "py_proto_library")

package(default_visibility = ["//visibility:public"])

py_proto_library(
    name = "python",
    srcs = glob(["*.proto"]),
    default_runtime = "@com_google_protobuf//:protobuf_python",
    protoc = "@com_google_protobuf//:protoc",
    deps = [
        "@com_google_protobuf//:protobuf_python",
    ],
)

java_proto_library(
    name = "java",
    deps = [":proto"],
)

proto_library(
    name = "proto",
    srcs = ["clics_v1.proto"],
    visibility = ["//visibility:private"],
    deps = [
        ":annotations",
        "@com_google_protobuf//:descriptor_proto",
        "@com_google_protobuf//:duration_proto",
        "@com_google_protobuf//:timestamp_proto",
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
        "@com_google_protobuf//:descriptor_proto",
    ],
)
