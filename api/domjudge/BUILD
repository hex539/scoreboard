load("@rules_java//java:defs.bzl", "java_binary", "java_library")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "domjudge",
    srcs = glob([
        "src/java/**",
    ]),
    exports = [
        "//api/domjudge/proto:java",
    ],
    deps = [
        "//api/domjudge/proto:annotations_java",
        "//api/domjudge/proto:java",
        "//api/utils",
        "//third_party/auto:auto_value",
        "//third_party/auto:auto_value_annotations",
        "//third_party/gson:prototype_adapter",
        "@protobuf//:protobuf_java",
        "@protobuf//:protobuf_java_util",
        "@maven//:com_google_code_gson_gson",
        "@maven//:com_google_guava_guava",
        "@maven//:com_squareup_okhttp3_okhttp",
        "@maven//:com_squareup_okio_okio",
    ],
)
