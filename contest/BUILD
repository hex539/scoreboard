package(default_visibility = ["//visibility:public"])

java_library(
  name = "contest",
  srcs = glob([
    "src/java/**",
  ]),
  deps = [
    "//api/clics",
    "//api/clics/proto:java",
    "//api/domjudge",
    "//api/domjudge/proto:java",
    "//api/utils",
    "//contest/proto:java",
    "//interop",
    "//third_party/auto:auto_value",
    "//third_party/auto:auto_value_annotations",
    "@com_google_protobuf//:protobuf_java",
    "@com_google_protobuf//:protobuf_java_util",
    "@guava_maven//jar",
    "@nullable//jar",
    "@okhttp//jar",
  ],
  exports = [
    "//api/clics/proto:java",
    "//api/domjudge/proto:java",
    "//contest/proto:java",
    "@com_google_protobuf//:protobuf_java",
    "@com_google_protobuf//:protobuf_java_util",
  ],
)
