package(default_visibility = ["//visibility:public"])

java_library(
  name = "contest",
  srcs = glob([
    "src/java/**",
  ]),
  deps = [
    "//clics/api",
    "//clics/proto:java",
    "//contest/proto:java",
    "//domjudge/api",
    "//domjudge/proto:java",
    "//interop",
    "@auto_value//:processor",
    "@com_google_protobuf//:protobuf_java",
    "@com_google_protobuf//:protobuf_java_util",
    "@okhttp//jar",
  ],
  exports = [
    "//clics/proto:java",
    "//domjudge/proto:java",
    "@com_google_protobuf//:protobuf_java",
    "@com_google_protobuf//:protobuf_java_util",
  ],
)
