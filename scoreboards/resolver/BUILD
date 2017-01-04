package(default_visibility = ["//visibility:private"])

java_binary(
  name = "resolver",
  srcs = glob(["src/java/me/hex539/resolver/*.java"]),
  main_class = "me.hex539.resolver.Executive",
  deps = [
    "//:scoreboard-lib",
    "//:domjudge-proto",
    "@jfoenix//jar",
  ],
)


