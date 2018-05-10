package(default_visibility = ["//visibility:private"])

java_binary(
  name = "progression",
  main_class = "me.hex539.analysis.Progression",
  resources = glob(["res/**"]),
  srcs = glob([
    "src/*.java",
  ]),
  deps = [
    "//contest",
    "//interop",
    "//third_party/org/jtwig",
    "@apachecommonslang//jar",
    "@com_google_protobuf//:protobuf_java",
    "@jewelcli//jar",
  ],
)
