package(default_visibility = ["//visibility:public"])

java_library(
  name = "domjudge",
  srcs = glob([
    "src/java/**",
  ]),
  deps = [
    "//api/domjudge/proto:annotations_java",
    "//api/domjudge/proto:java",
    "//api/utils",
    "//external:gson",
    "//external:guava",
    "//third_party/auto:auto_value",
    "//third_party/auto:auto_value_annotations",
    "//third_party/gson:prototype_adapter",
    "@com_google_protobuf//:protobuf_java",
    "@com_google_protobuf//:protobuf_java_util",
    "@okhttp//jar",
    "@okio//jar",
  ]
)
