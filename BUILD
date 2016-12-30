load("@org_pubref_rules_protobuf//java:rules.bzl", "java_proto_library")

java_binary(
  name = "console",
  srcs = glob(["src/me/hex539/console/*.java"]),
  main_class = "me.hex539.console.Executive",
  deps = [
    ":scoreboard-lib",
    ":domjudge-proto",
    "@jewelcli//jar",
    "@okhttp//jar",
    "@okio//jar",
    "@gson//jar",
  ],
)

java_library(
  name = "scoreboard-lib",
  srcs = glob(["src/me/hex539/scoreboard/*.java"]),
  deps = [
    ":domjudge-proto",
    "@gson//jar",
  ],
)

java_proto_library(
  name = "domjudge-proto",
  protos = ["src/me/hex539/proto/domjudge.proto"],
)
