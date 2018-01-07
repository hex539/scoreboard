package(default_visibility = ["//visibility:public"])

DEPS = [
    "//domjudge/proto:proto",
    "//domjudge/proto:annotations",
    "//domjudge/proto:annotations_compile_imports",
    "//third_party/gson:lib",
    "@auto_value//:processor",
    "@google_protobuf//:protobuf_java",
    "@gson//jar",
    "@okhttp//jar",
    "@okio//jar",
]

android_library(
  name = "android",
  srcs = glob([
    "src/java/**",
  ]),
  deps = DEPS,
)

java_library(
  name = "api",
  srcs = glob([
    "src/java/**"
  ]),
  deps = DEPS,
)
