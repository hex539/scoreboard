package(default_visibility = ["//visibility:private"])

java_binary(
  name = "console",
  main_class = "me.hex539.console.Executive",
  srcs = glob([
    "src/java/me/hex539/console/*.java",
  ]),
  deps = [
    "//clics/api",
    "//clics/proto",
    "//contest",
    "//interop",
    "//domjudge/api",
    "//domjudge/proto",
    "@jewelcli//jar",
  ],
)


