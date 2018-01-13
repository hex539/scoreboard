package(default_visibility = ["//visibility:public"])

android_library(
  name = "android",
  srcs = glob([
    "src/java/**",
  ]),
  deps = [
    "//clics/api:android",
    "//clics/proto",
    "//domjudge/api:android",
    "//domjudge/proto",
    "//interop:android",
    "@auto_value//:processor",
  ]
)

java_library(
  name = "contest",
  srcs = glob([
    "src/java/**",
  ]),
  deps = [
    "//clics/api",
    "//clics/proto",
    "//domjudge/api",
    "//domjudge/proto",
    "//interop",
    "@auto_value//:processor",
  ]
)
