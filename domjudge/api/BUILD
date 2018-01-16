package(default_visibility = ["//visibility:public"])

java_library(
  name = "api",
  srcs = glob([
    "src/java/**"
  ]),
  deps = [
    "//domjudge/proto:proto",
    "//domjudge/proto:annotations",
    "//domjudge/proto:annotations_compile_imports",
    "//third_party/gson:lib",
    "@auto_value//:processor",
    "@com_google_protobuf//:protobuf_java",
    "@gson//jar",
    "@okhttp//jar",
    "@okio//jar",
  ]
)
