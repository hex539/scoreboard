package(default_visibility = ["//visibility:public"])

licenses(["restricted"])

java_library(
    name = "prototype_adapter",
    srcs = glob([
        "proto/src/**/*.java",
    ]),
    deps = [
        "@maven//:com_google_code_gson_gson",
        "@maven//:com_google_guava_guava",
        "@com_google_protobuf//:protobuf_java",
    ],
)
