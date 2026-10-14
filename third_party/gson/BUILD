load("@rules_java//java:defs.bzl", "java_binary", "java_library")

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
        "@protobuf//:protobuf_java",
    ],
)
