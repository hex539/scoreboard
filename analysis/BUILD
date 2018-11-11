package(default_visibility = ["//visibility:private"])

java_binary(
  name = "progression",
  main_class = "me.hex539.analysis.Progression",
  resources = glob(["res/**"]),
  srcs = [
    "src/Invocation.java",
    "src/Progresion.java",
  ],
  deps = [
    "//contest",
    "//interop",
    "//third_party/org/jtwig",
    "@apachecommonslang//jar",
    "@com_google_protobuf//:protobuf_java",
    "@jewelcli//jar",
  ],
)

java_binary(
  name = "activity",
  main_class = "me.hex539.analysis.Activity",
  resources = glob(["res/**"]),
  srcs = [
    "src/Invocation.java",
    "src/Activity.java",
  ],
  deps = [
    "//contest",
    "//interop",
    "//third_party/org/jtwig",
    "@apachecommonslang//jar",
    "@com_google_protobuf//:protobuf_java",
    "@jewelcli//jar",
  ],
)
