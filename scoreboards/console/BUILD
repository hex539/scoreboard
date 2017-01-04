package(default_visibility = ["//visibility:public"])

java_binary(
  name = "console",
  srcs = glob(["src/java/me/hex539/console/*.java"]),
  main_class = "me.hex539.console.Executive",
  deps = [
    "//:scoreboard-lib",
    "//:domjudge-proto",
    "@jewelcli//jar",
  ],
)


