package(default_visibility = ["//visibility:public"])

java_library(
  name = "contest",
  exports = [
    "//api/clics/proto:java",
    "//contest/proto:java",
    ":comparators",
    ":judgement_dispatcher",
    ":immutable",
    ":model",
    ":mutable",
    ":network",
    ":resolver",
  ],
)

java_library(
  name = "network",
  srcs = glob([
    "src/java/me/hex539/contest/ApiDetective.java",
    "src/java/me/hex539/contest/ContestDownloader.java",
    "src/java/me/hex539/contest/EventFeedController.java",
  ]),
  deps = [
    "//api/clics",
    "//api/domjudge",
    "//api/utils",
    "//contest/proto:java",
    "//interop",
    "@com_google_protobuf//:protobuf_java",
    "@com_google_protobuf//:protobuf_java_util",
    "@okhttp//jar",
    ":judgement_dispatcher",
    ":immutable",
    ":model",
    ":mutable",
  ],
  exports = [
    "//api/clics/proto:java",
    "//contest/proto:java",
  ],
)

java_library(
  name = "resolver",
  srcs = [
    "src/java/me/hex539/contest/ObserverCapturer.java",
    "src/java/me/hex539/contest/ResolverController.java",
  ],
  deps = [
    "//third_party/auto:auto_value",
    "//third_party/auto:auto_value_annotations",
    "@com_google_protobuf//:protobuf_java",
    "@com_google_protobuf//:protobuf_java_util",
    "@guava_maven//jar",
    "@nullable//jar",
    ":comparators",
    ":immutable",
    ":judgement_dispatcher",
    ":model",
    ":mutable",
  ],
)

java_library(
  name = "judgement_dispatcher",
  srcs = glob([
    "src/java/me/hex539/contest/JudgementDispatcher.java",
    "src/java/me/hex539/contest/MissingJudgements.java",
  ]),
  deps = [
    "@com_google_protobuf//:protobuf_java",
    "@com_google_protobuf//:protobuf_java_util",
    ":comparators",
    ":model",
    ":mutable",
  ],
  exports = [
    ":model",
  ],
)

java_library(
  name = "comparators",
  srcs = [
    "src/java/me/hex539/contest/Comparators.java",
  ],
  deps = [
    "//api/clics/proto:java",
    ":model",
  ],
  exports = [
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
  deps = [
    "//api/clics/proto:java",
  ],
  exports = [
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
    "//api/clics/proto:java",
    "//third_party/auto:auto_value",
    "//third_party/auto:auto_value_annotations",
    "@com_google_protobuf//:protobuf_java",
    "@com_google_protobuf//:protobuf_java_util",
    "@guava_maven//jar",
    ":comparators",
    ":data_structures",
    ":model",
  ],
)

java_library(
  name = "immutable",
  srcs = glob([
    "src/java/me/hex539/contest/immutable/*.java",
    "src/java/me/hex539/contest/ImmutableScoreboardModel.java",
  ]),
  deps = [
    "//api/clics/proto:java",
    "//third_party/auto:auto_value",
    "//third_party/auto:auto_value_annotations",
    "@guava_maven//jar",
    "@nullable//jar",
    ":comparators",
    ":model",
  ],
)
