package(default_visibility = ["//visibility:private"])

java_binary(
    name = "resolver",
    srcs = glob([
        "src/java/**/*.java",
    ]),
    main_class = "me.hex539.resolver.Executive",
    deps = [
        ":resources",
        "//contest",
        "//interop",
        "//third_party/auto:auto_value",
        "//third_party/auto:auto_value_annotations",
        "@maven//:com_lexicalscope_jewelcli_jewelcli",
        "@maven//:org_lwjgl_lwjgl",
        "@maven//:org_lwjgl_lwjgl_glfw",
        "@maven//:org_lwjgl_lwjgl_opengl",
        "@maven//:org_lwjgl_lwjgl_stb",
    ],
    runtime_deps = [
        "@maven//:org_lwjgl_lwjgl_natives_linux",
        "@maven//:org_lwjgl_lwjgl_glfw_natives_linux",
        "@maven//:org_lwjgl_lwjgl_opengl_natives_linux",
        "@maven//:org_lwjgl_lwjgl_stb_natives_linux",
    ] + [
        "@maven//:org_lwjgl_lwjgl_natives_macos",
        "@maven//:org_lwjgl_lwjgl_glfw_natives_macos",
        "@maven//:org_lwjgl_lwjgl_opengl_natives_macos",
        "@maven//:org_lwjgl_lwjgl_stb_natives_macos",
    ] + [
        "@maven//:org_lwjgl_lwjgl_natives_windows",
        "@maven//:org_lwjgl_lwjgl_glfw_natives_windows",
        "@maven//:org_lwjgl_lwjgl_opengl_natives_windows",
        "@maven//:org_lwjgl_lwjgl_stb_natives_windows",
    ],
)

java_library(
    name = "resources",
    resources = glob([
        "src/resources/**",
    ]),
)
