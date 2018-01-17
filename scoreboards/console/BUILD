package(default_visibility = ["//visibility:private"])

java_binary(
  name = "console",
  main_class = "me.hex539.console.Executive",
  srcs = glob([
    "src/java/me/hex539/console/*.java",
  ]),
  deps = [
    "//contest",
    "//interop",
    "@com_google_protobuf//:protobuf_java",
    "@jewelcli//jar",
  ],
)


