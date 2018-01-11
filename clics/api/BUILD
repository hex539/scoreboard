package(default_visibility = ["//visibility:public"])

DEPS = [
    "//clics/proto:proto",
    "//clics/proto:annotations",
    "//clics/proto:annotations_compile_imports",
    "//third_party/gson:lib",
    "@auto_value//:processor",
    "@com_google_protobuf//:protobuf_java",
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
