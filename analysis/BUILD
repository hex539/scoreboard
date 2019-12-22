package(default_visibility = ["//visibility:private"])

java_binary(
    name = "progression",
    srcs = [
        "src/Invocation.java",
        "src/Progression.java",
    ],
    main_class = "me.hex539.analysis.Progression",
    resources = glob(["res/**"]),
    deps = [
        ":common",
        "//third_party/org/jtwig",
    ],
)

java_binary(
    name = "activity",
    srcs = [
        "src/Activity.java",
        "src/Invocation.java",
        "src/SubmitStats.java",
    ],
    main_class = "me.hex539.analysis.Activity",
    resources = glob(["res/**"]),
    deps = [
        ":common",
        "//third_party/org/jtwig",
    ],
)

COMMON_DEPS = [
    "@maven//:com_lexicalscope_jewelcli_jewelcli",
    "@com_google_protobuf//:protobuf_java",
    "@com_google_protobuf//:protobuf_java_util",
    "@maven//:org_apache_commons_commons_lang3",
    "//contest",
    "//interop",
]

java_library(
    name = "common",
    srcs = [
        "src/Analyser.java",
        "src/Invocation.java",
    ],
    exports = COMMON_DEPS,
    deps = COMMON_DEPS,
)
