package(default_visibility = ["//visibility:private"])

java_binary(
  name = "resolver",
  main_class = "me.hex539.resolver.Executive",
  srcs = glob([
    "src/java/**/*.java",
  ]),
  resources = glob([
    "src/resources/**",
  ]),
  deps = [
    "//contest",
    "//interop",
    "//third_party/auto:auto_value",
    "//third_party/auto:auto_value_annotations",
    "@jewelcli//jar",
    "@lwjgl//jar",
    "@lwjgl_glfw//jar",
    "@lwjgl_opengl//jar",
    "@lwjgl_stb//jar",
  ],
  runtime_deps = [
    "@lwjgl_natives_linux//jar",
    "@lwjgl_glfw_natives_linux//jar",
    "@lwjgl_opengl_natives_linux//jar",
    "@lwjgl_stb_natives_linux//jar",
  ] + [
    "@lwjgl_natives_windows//jar",
    "@lwjgl_glfw_natives_windows//jar",
    "@lwjgl_opengl_natives_windows//jar",
    "@lwjgl_stb_natives_windows//jar",
  ],
)
