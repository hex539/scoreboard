workspace(name = "scoreboard")

load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

RULES_JVM_EXTERNAL_TAG = "3.0"
RULES_JVM_EXTERNAL_SHA = "62133c125bf4109dfd9d2af64830208356ce4ef8b165a6ef15bbff7460b35c3a"

http_archive(
    name = "rules_jvm_external",
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    sha256 = RULES_JVM_EXTERNAL_SHA,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)

load("@rules_jvm_external//:defs.bzl", "artifact")
load("@rules_jvm_external//:specs.bzl", "maven")
load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    name = "maven",
    artifacts = [
        "com.android.support:cardview-v7:28.0.0",
        "com.android.support:leanback-v17:28.0.0",
        "com.android.support:preference-leanback-v17:28.0.0",
        "com.android.support:preference-v7:28.0.0",
        "com.android.support:recyclerview-v7:28.0.0",
        "com.android.support:support-compat:28.0.0",
        "com.android.support:support-fragment:28.0.0",
        "com.google.auto:auto-common:0.10",
        "com.google.auto.value:auto-value:1.6.3",
        "com.google.auto.value:auto-value-annotations:1.6.3",
        "com.googlecode.concurrentlinkedhashmap:concurrentlinkedhashmap-lru:1.4.2",
        "com.google.code.findbugs:jsr305:3.0.2",
        "com.google.code.gson:gson:2.8.2",
        "com.google.errorprone:error_prone_annotations:2.3.2",
        "com.google.guava:guava:24.0-android",
        "com.google.truth:truth:0.27",
        "com.jfoenix:jfoenix:1.0.0",
        "com.lexicalscope.jewelcli:jewelcli:0.8.9",
        "com.squareup.okhttp3:mockwebserver:3.12.0",
        "com.squareup.okhttp3:okhttp:3.12.0",
        "com.squareup.okio:okio:1.13.0",
        "junit:junit:4.12",
        "org.apache.commons:commons-lang3:3.7",
        "org.jtwig:jtwig-core:5.87.0.RELEASE",
        "org.jtwig:jtwig-reflection:5.87.0.RELEASE",
        "org.lwjgl:lwjgl:3.2.2",
        "org.lwjgl:lwjgl-glfw:3.2.2",
        "org.lwjgl:lwjgl-opengl:3.2.2",
        "org.lwjgl:lwjgl-stb:3.2.2",
        "org.mockito:mockito-all:1.10.19",
        "org.ow2.asm:asm:5.2",
        "org.ow2.asm:asm-analysis:5.2",
        "org.ow2.asm:asm-tree:5.2",
        "org.ow2.asm:asm-util:5.2",
        "org.parboiled:parboiled-core:1.1.8",
        "org.parboiled:parboiled-java:1.1.8",
        "org.slf4j:slf4j-api:1.7.25",
        "org.slf4j:slf4j-simple:1.7.25",
        maven.artifact(group = "org.lwjgl", artifact = "lwjgl", classifier = "natives-linux", version = "3.2.2"),
        maven.artifact(group = "org.lwjgl", artifact = "lwjgl-glfw", classifier = "natives-linux", version = "3.2.2"),
        maven.artifact(group = "org.lwjgl", artifact = "lwjgl-opengl", classifier = "natives-linux", version = "3.2.2"),
        maven.artifact(group = "org.lwjgl", artifact = "lwjgl-stb", classifier = "natives-linux", version = "3.2.2"),
        maven.artifact(group = "org.lwjgl", artifact = "lwjgl", classifier = "natives-macos", version = "3.2.2"),
        maven.artifact(group = "org.lwjgl", artifact = "lwjgl-glfw", classifier = "natives-macos", version = "3.2.2"),
        maven.artifact(group = "org.lwjgl", artifact = "lwjgl-opengl", classifier = "natives-macos", version = "3.2.2"),
        maven.artifact(group = "org.lwjgl", artifact = "lwjgl-stb", classifier = "natives-macos", version = "3.2.2"),
        maven.artifact(group = "org.lwjgl", artifact = "lwjgl", classifier = "natives-windows", version = "3.2.2"),
        maven.artifact(group = "org.lwjgl", artifact = "lwjgl-glfw", classifier = "natives-windows", version = "3.2.2"),
        maven.artifact(group = "org.lwjgl", artifact = "lwjgl-opengl", classifier = "natives-windows", version = "3.2.2"),
        maven.artifact(group = "org.lwjgl", artifact = "lwjgl-stb", classifier = "natives-windows", version = "3.2.2"),
    ],
    repositories = [
        "https://jcenter.bintray.com",
        "https://maven.google.com",
        "https://repo1.maven.org/maven2",
    ],
    fetch_sources = True,
    version_conflict_policy = "pinned",
    generate_compat_repositories = True,
)

android_sdk_repository(
    name = "androidsdk",
)

skylib_version = "0.8.0"
http_archive(
    name = "bazel_skylib",
    type = "tar.gz",
    url = "https://github.com/bazelbuild/bazel-skylib/releases/download/{}/bazel-skylib.{}.tar.gz".format (skylib_version, skylib_version),
    sha256 = "2ef429f5d7ce7111263289644d233707dba35e39696377ebab8b0bc701f7818e",
)

load("@bazel_skylib//:workspace.bzl", "bazel_skylib_workspace")
bazel_skylib_workspace()

bind(
    name = "gson",
    actual = "@maven//:com_google_code_gson_gson"
)
bind(
    name = "guava",
    actual = "@maven//:com_google_guava_guava"
)
bind(
    name = "error_prone_annotations",
    actual = "@maven//:com_google_errorprone_error_prone_annotations"
)
git_repository(
    name = 'com_google_protobuf',
    remote = 'https://github.com/protocolbuffers/protobuf',
    commit = "4457b1f2905d0a0db9c8f85d2b375340e8adcd9f",
    shallow_since = "1558721209 -0700",
)

load("@com_google_protobuf//:protobuf_deps.bzl", "protobuf_deps")
protobuf_deps()

git_repository(
    name = "org_pubref_rules_maven",
    remote = "https://github.com/pubref/rules_maven",
    commit = "339c378f856461add63f155d82077de5813e649e",
    shallow_since = "1555185650 -0600",
)

load("@org_pubref_rules_maven//maven:rules.bzl", "maven_repositories")
maven_repositories()

load("@org_pubref_rules_maven//maven:rules.bzl", "maven_repository")
maven_repository(
    name = "parboiled_java",
    deps = ["org.parboiled:parboiled-java:1.1.8"],
    transitive_deps = [
        '4ce3ecdc7115bcbf9d4ff4e6ec638e60760819df:org.ow2.asm:asm:5.2',
        '2de10833bb3ade1939b1489b7656c9de20343e14:org.ow2.asm:asm-analysis:5.2',
        '733a8d67f6f4174d12142b7bbcfc496a6d99882e:org.ow2.asm:asm-tree:5.2',
        '9408ea14e73b7c9b427545a1b84923d6afaf8e1e:org.ow2.asm:asm-util:5.2',
        'af12604d2e555c65107d744bc502dcd9d1c0cad9:org.parboiled:parboiled-core:1.1.8',
        'df6ca179d7a1cd0b556a3f46c219503d0ab80388:org.parboiled:parboiled-java:1.1.8',
    ],
)

load("@parboiled_java//:rules.bzl", "parboiled_java_compile")
parboiled_java_compile()
