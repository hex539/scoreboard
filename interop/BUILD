load("@rules_java//java:defs.bzl", "java_library")
package(default_visibility = ["//visibility:public"])

java_library(
    name = "interop",
    srcs = glob([
        "src/java/**",
    ]),
    deps = [
        "//api/clics/proto:java",
        "//api/domjudge/proto:java",
        "@protobuf//:protobuf_java",
        "@protobuf//:protobuf_java_util",
    ],
)
