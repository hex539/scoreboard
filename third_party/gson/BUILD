package(default_visibility = ["//visibility:public"])
licenses(["restricted"])

java_library(
  name = "prototype_adapter",
  srcs = glob([
    "proto/src/**/*.java",
  ]),
  deps = [
    "@com_google_protobuf//:protobuf_java",
    "//external:gson",
    "//external:guava",
  ],
)
