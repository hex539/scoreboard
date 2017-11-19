package(default_visibility = ["//visibility:public"])

android_library(
  name = "android",
  srcs = glob([
    "src/java/**",
  ]),
  deps = [
    "//domjudge/proto:proto",
    "//domjudge/proto:annotations",
    "//domjudge/proto:annotations_compile_imports",
    "//third_party/gson:lib",
    "@auto_value//:processor",
    "@google_protobuf//:protobuf_java",
    "@gson//jar",
    "@okhttp//jar",
    "@okio//jar",
  ],
)

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
    "@google_protobuf//:protobuf_java",
    "@gson//jar",
    "@okhttp//jar",
    "@okio//jar",
  ],
)
