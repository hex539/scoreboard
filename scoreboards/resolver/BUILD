package(default_visibility = ["//visibility:private"])

java_binary(
  name = "resolver",
  main_class = "me.hex539.resolver.Executive",
  srcs = glob([
    "src/java/**/*.java",
  ]),
  resources = glob([
    "src/resources/**",
  ]),
  deps = [
    "//clics/api",
    "//clics/proto",
    "//domjudge/api",
    "//contest",
    "//interop",
    "@jfoenix//jar",
  ],
)


