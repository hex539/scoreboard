package(default_visibility = ["//visibility:public"])

java_library(
    name = "utils",
    srcs = glob([
        "src/java/**",
    ]),
    deps = [
        "//third_party/gson:prototype_adapter",
        "@maven//:com_google_code_gson_gson",
        "@maven//:com_google_guava_guava",
        "@maven//:com_squareup_okhttp3_okhttp",
        "@maven//:com_squareup_okio_okio",
    ],
)
