package(default_visibility = ["//visibility:public"])

java_library(
    name = "contest",
    exports = [
        ":comparators",
        ":immutable",
        ":judgement_dispatcher",
        ":model",
        ":mutable",
        ":network",
        ":resolver",
        "//api/clics/proto:java",
        "//contest/proto:java",
    ],
)

java_library(
    name = "network",
    srcs = glob([
        "src/java/me/hex539/contest/ApiDetective.java",
        "src/java/me/hex539/contest/ContestDownloader.java",
        "src/java/me/hex539/contest/EventFeedController.java",
    ]),
    exports = [
        "//api/clics/proto:java",
        "//contest/proto:java",
    ],
    deps = [
        ":immutable",
        ":judgement_dispatcher",
        ":model",
        ":mutable",
        "//api/clics",
        "//api/domjudge",
        "//api/utils",
        "//contest/proto:java",
        "//interop",
        "@com_google_protobuf//:protobuf_java",
        "@com_google_protobuf//:protobuf_java_util",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_squareup_okhttp3_okhttp",
    ],
)

java_library(
    name = "resolver",
    srcs = [
        "src/java/me/hex539/contest/ObserverCapturer.java",
        "src/java/me/hex539/contest/ResolverController.java",
    ],
    deps = [
        ":comparators",
        ":immutable",
        ":judgement_dispatcher",
        ":model",
        ":mutable",
        "//third_party/auto:auto_value",
        "//third_party/auto:auto_value_annotations",
        "@com_google_protobuf//:protobuf_java",
        "@com_google_protobuf//:protobuf_java_util",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
    ],
)

java_library(
    name = "judgement_dispatcher",
    srcs = glob([
        "src/java/me/hex539/contest/JudgementDispatcher.java",
        "src/java/me/hex539/contest/MissingJudgements.java",
    ]),
    exports = [
        ":model",
    ],
    deps = [
        ":comparators",
        ":model",
        ":mutable",
        "@com_google_protobuf//:protobuf_java",
        "@com_google_protobuf//:protobuf_java_util",
    ],
)

java_library(
    name = "comparators",
    srcs = [
        "src/java/me/hex539/contest/Comparators.java",
    ],
    exports = [
        "//api/clics/proto:java",
    ],
    deps = [
        ":model",
        "//api/clics/proto:java",
    ],
)

java_library(
    name = "data_structures",
    srcs = [
        "src/java/me/hex539/contest/SplayList.java",
        "src/java/me/hex539/contest/SplayTree.java",
    ],
)

java_library(
    name = "model",
    srcs = glob([
        "src/java/me/hex539/contest/model/*.java",
        "src/java/me/hex539/contest/ScoreboardModel.java",
    ]),
    exports = [
        "//api/clics/proto:java",
    ],
    deps = [
        "//api/clics/proto:java",
    ],
)

java_library(
    name = "mutable",
    srcs = glob([
        "src/java/me/hex539/contest/mutable/*.java",
        "src/java/me/hex539/contest/ScoreboardModelImpl.java",
    ]),
    deps = [
        ":comparators",
        ":data_structures",
        ":model",
        "//api/clics/proto:java",
        "//third_party/auto:auto_value",
        "//third_party/auto:auto_value_annotations",
        "@com_google_protobuf//:protobuf_java",
        "@com_google_protobuf//:protobuf_java_util",
        "@maven//:com_google_guava_guava",
    ],
)

java_library(
    name = "immutable",
    srcs = glob([
        "src/java/me/hex539/contest/immutable/*.java",
        "src/java/me/hex539/contest/ImmutableScoreboardModel.java",
    ]),
    deps = [
        ":comparators",
        ":model",
        "//api/clics/proto:java",
        "//third_party/auto:auto_value",
        "//third_party/auto:auto_value_annotations",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
    ],
)
