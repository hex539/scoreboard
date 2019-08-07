package(default_visibility = ["//visibility:private"])

java_binary(
  name = "progression",
  main_class = "me.hex539.analysis.Progression",
  resources = glob(["res/**"]),
  srcs = [
    "src/Invocation.java",
    "src/Progression.java",
  ],
  deps = [
    ":common",
    "//third_party/org/jtwig",
  ]
)

java_binary(
  name = "activity",
  main_class = "me.hex539.analysis.Activity",
  resources = glob(["res/**"]),
  srcs = [
    "src/Activity.java",
    "src/Invocation.java",
    "src/SubmitStats.java",
  ],
  deps = [
    ":common",
    "//third_party/org/jtwig",
  ],
)

COMMON_DEPS = [
  "@jewelcli//jar",
  "@com_google_protobuf//:protobuf_java",
  "@com_google_protobuf//:protobuf_java_util",
  "@apachecommonslang//jar",
  "//contest",
  "//interop",
]

java_library(
  name = "common",
  srcs = [
    "src/Invocation.java",
    "src/Analyser.java",
  ],
  deps = COMMON_DEPS,
  exports = COMMON_DEPS,
)
