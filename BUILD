load("@org_pubref_rules_protobuf//java:rules.bzl", "java_proto_library")

java_binary(
  name = "console",
  srcs = glob(["src/me/hex539/console/*.java"]),
  main_class = "me.hex539.console.Executive",
  deps = [
    ":scoreboard-lib",
    ":domjudge-proto",
    "@jewelcli//jar",
  ],
)

java_binary(
  name = "resolver",
  srcs = glob(["src/me/hex539/resolver/*.java"]),
  main_class = "me.hex539.resolver.Executive",
  deps = [
    ":scoreboard-lib",
    ":domjudge-proto",
    "@jfoenix//jar",
  ],
)

java_library(
  name = "scoreboard-lib",
  srcs = glob(["src/me/hex539/scoreboard/*.java"]),
  deps = [
    ":domjudge-proto",
    ":annotations-proto",
    ":annotations-proto_compile_imports",
    "//third_party/gson:lib",
    "@google_protobuf//:protobuf_java",
    "@okhttp//jar",
    "@okio//jar",
    "@gson//jar",
  ],
)

java_proto_library(
  name = "domjudge-proto",
  protos = ["src/me/hex539/proto/domjudge.proto"],
  proto_deps = [":annotations-proto"],
  imports = [
    "external/google_protobuf/src",
    "src/me/hex539/proto"
  ],
)

java_proto_library(
  name = "annotations-proto",
  protos = ["src/me/hex539/proto/annotations.proto"],
  imports = [
    "external/google_protobuf/src"
  ],
  inputs = [
    "@google_protobuf//:well_known_protos"
  ],
)
