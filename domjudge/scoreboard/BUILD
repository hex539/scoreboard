package(default_visibility = ["//visibility:public"])

DEPS = [
  "//domjudge/api",
  "//domjudge/proto",
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
  name = "scoreboard",
  srcs = glob([
    "src/java/**"
  ]),
  deps = DEPS,
)
