package(default_visibility = ["//visibility:public"])

java_library(
    name = "clics",
    srcs = glob([
        "src/java/**",
    ]),
    exports = [
        "//api/clics/proto:java",
    ],
    deps = [
        "//api/clics/proto:annotations_java",
        "//api/clics/proto:java",
        "//api/utils",
        "//third_party/auto:auto_value",
        "//third_party/auto:auto_value_annotations",
        "//third_party/gson:prototype_adapter",
        "@com_google_protobuf//:protobuf_java",
        "@com_google_protobuf//:protobuf_java_util",
        "@maven//:com_google_code_gson_gson",
        "@maven//:com_google_guava_guava",
        "@maven//:com_squareup_okhttp3_okhttp",
        "@maven//:com_squareup_okio_okio",
    ],
)
