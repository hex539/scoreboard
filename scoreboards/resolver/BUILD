package(default_visibility = ["//visibility:private"])

java_binary(
  name = "resolver",
  main_class = "me.hex539.resolver.Executive",
  srcs = glob([
    "src/java/**/*.java",
  ]),
  deps = [
    "//domjudge/api:domjudge-api",
    "//domjudge/proto:domjudge-proto",
    "@jfoenix//jar",
  ],
)


