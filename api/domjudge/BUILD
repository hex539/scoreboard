package(default_visibility = ["//visibility:public"])

java_library(
  name = "domjudge",
  srcs = glob([
    "src/java/**",
  ]),
  deps = [
    "//api/domjudge/proto:java",
    "//api/domjudge/proto:annotations_java",
    "//api/utils",
    "//third_party/gson:prototype_adapter",
    "//external:gson",
    "//external:guava",
    "@auto_value//:processor",
    "@com_google_protobuf//:protobuf_java",
    "@okhttp//jar",
    "@okio//jar",
  ]
)
