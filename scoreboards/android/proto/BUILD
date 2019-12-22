package(default_visibility = ["//visibility:public"])

java_library(
    name = "java",
    exports = [":java_internal"],
)

java_proto_library(
    name = "java_internal",
    visibility = ["//visibility:private"],
    deps = [":proto"],
)

proto_library(
    name = "proto",
    srcs = ["config.proto"],
    visibility = ["//visibility:private"],
    deps = [
        "//contest/proto",
        "@com_google_protobuf//:descriptor_proto",
        "@com_google_protobuf//:duration_proto",
        "@com_google_protobuf//:timestamp_proto",
    ],
)
