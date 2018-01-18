package(default_visibility = ["//visibility:public"])

java_library(
  name = "api",
  srcs = glob([
    "src/java/**",
  ]),
  deps = [
    "//domjudge/proto:java",
    "//domjudge/proto:annotations_java",
    "//third_party/gson:prototype_adapter",
    "//external:gson",
    "//external:guava",
    "@auto_value//:processor",
    "@com_google_protobuf//:protobuf_java",
    "@okhttp//jar",
    "@okio//jar",
  ]
)
