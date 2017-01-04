package(default_visibility = ["//visibility:public"])

java_library(
  name = "domjudge-api",
  srcs = glob([
    "src/java/**/*.java"
  ]),
  deps = [
    "//domjudge/proto:domjudge-proto",
    "//domjudge/proto:annotations-proto",
    "//domjudge/proto:annotations-proto_compile_imports",
    "//third_party/gson:lib",
    "@google_protobuf//:protobuf_java",
    "@okhttp//jar",
    "@okio//jar",
    "@gson//jar",
  ],
)

