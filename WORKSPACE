workspace(name = "scoreboard")

android_sdk_repository(
  name = "androidsdk",
  path = "/usr/local/android-sdk",
  api_level = 26,
  build_tools_version = "26.0.3",
)

git_repository(
  name = "org_pubref_rules_protobuf",
  remote = "https://github.com/pubref/rules_protobuf",
  tag = "v0.8.1",
)
load("@org_pubref_rules_protobuf//java:rules.bzl", "java_proto_repositories")
java_proto_repositories()

git_repository(
  name = "google_protobuf",
  remote = "https://github.com/google/protobuf",
  tag = "v3.5.1",
)

maven_jar(
  name = "jewelcli",
  artifact = "com.lexicalscope.jewelcli:jewelcli:0.8.9",
)

maven_jar(
  name = "okhttp",
  artifact = "com.squareup.okhttp3:okhttp:3.8.1",
)

maven_jar(
  name = "okio",
  artifact = "com.squareup.okio:okio:1.13.0",
)

maven_jar(
  name = "gson",
  artifact = "com.google.code.gson:gson:2.8.1",
)

maven_jar(
  name = "guava",
  artifact = "com.google.guava:guava:22.0-android",
)

maven_jar(
  name = "jfoenix",
  artifact = "com.jfoenix:jfoenix:1.0.0",
)

maven_jar(
    name = "junit4",
    artifact = "junit:junit:4.12",
)

maven_jar(
    name = "mockito",
    artifact = "org.mockito:mockito-all:1.10.19",
)

maven_jar(
    name = "truth",
    artifact = "com.google.truth:truth:0.27",
)

new_http_archive(
    name = "auto_value",
    url = "http://repo1.maven.org/maven2/com/google/auto/value/auto-value/1.4/auto-value-1.4.jar",
    build_file_content = """
java_import(
    name = "jar",
    jars = ["auto-value-1.4.jar"],
)

java_plugin(
    name = "autovalue-plugin",
    generates_api = 1,
    processor_class = "com.google.auto.value.processor.AutoValueProcessor",
    deps = [":jar"],
)

java_library(
    name = "processor",
    exported_plugins = [":autovalue-plugin"],
    exports = [":jar"],
    visibility = ["//visibility:public"],
)
""",
)
