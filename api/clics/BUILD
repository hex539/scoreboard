package(default_visibility = ["//visibility:public"])

java_library(
  name = "clics",
  srcs = glob([
    "src/java/**"
  ]),
  deps = [
    "//api/clics/proto:java",
    "//api/clics/proto:annotations_java",
    "//api/utils",
    "//third_party/gson:prototype_adapter",
    "//external:gson",
    "//external:guava",
    "@auto_value//:processor",
    "@com_google_protobuf//:protobuf_java",
    "@com_google_protobuf//:protobuf_java_util",
    "@okhttp//jar",
    "@okio//jar",
  ],
)
