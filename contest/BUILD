package(default_visibility = ["//visibility:public"])

DEPS = [
  "//clics/api",
  "//clics/proto",
  "//domjudge/api",
  "//domjudge/proto",
  "//interop",
  "@auto_value//:processor",
]

android_library(
  name = "android",
  srcs = glob([
    "src/java/**",
  ]),
  deps = DEPS,
)

java_library(
  name = "contest",
  srcs = glob([
    "src/java/**",
  ]),
  deps = DEPS,
)
