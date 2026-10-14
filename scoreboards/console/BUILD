load("@rules_java//java:defs.bzl", "java_binary", "java_library")

package(default_visibility = ["//visibility:private"])

java_binary(
    name = "console",
    srcs = glob([
        "src/java/me/hex539/console/*.java",
    ]),
    main_class = "me.hex539.console.Executive",
    deps = [
        "//contest",
        "//interop",
        "@protobuf//:protobuf_java",
        "@protobuf//:protobuf_java_util",
        "@maven//:com_lexicalscope_jewelcli_jewelcli",
    ],
)
