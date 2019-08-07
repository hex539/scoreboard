package(default_visibility = ["//visibility:public"])

java_library(
  name = "clics",
  srcs = glob([
    "src/java/**"
  ]),
  deps = [
    "//api/clics/proto:annotations_java",
    "//api/clics/proto:java",
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
  ],
  exports = [
    "//api/clics/proto:java",
  ],
)
