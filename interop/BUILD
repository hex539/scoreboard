package(default_visibility = ["//visibility:public"])

DEPS = [
    "//clics/proto",
    "//domjudge/proto",
]

android_library(
  name = "android",
  srcs = glob([
    "src/java/**",
  ]),
  deps = DEPS,
)

java_library(
  name = "interop",
  srcs = glob([
    "src/java/**"
  ]),
  deps = DEPS,
)
