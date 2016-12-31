workspace(name = "scoreboard")

git_repository(
  name = "org_pubref_rules_protobuf",
  remote = "https://github.com/pubref/rules_protobuf",
  tag = "v0.7.1",
)
load("@org_pubref_rules_protobuf//java:rules.bzl", "java_proto_repositories")
java_proto_repositories()

git_repository(
  name = "google_protobuf",
  remote = "https://github.com/google/protobuf",
  tag = "v3.1.0",
)

maven_jar(
  name = "jewelcli",
  artifact = "com.lexicalscope.jewelcli:jewelcli:0.8.3",
)

maven_jar(
  name = "okhttp",
  artifact = "com.squareup.okhttp3:okhttp:3.5.0",
)

maven_jar(
  name = "okio",
  artifact = "com.squareup.okio:okio:1.11.0",
)

maven_jar(
  name = "gson",
  artifact = "com.google.code.gson:gson:2.6.2",
)

maven_jar(
  name = "guava",
  artifact = "com.google.guava:guava:21.0-rc1",
)
