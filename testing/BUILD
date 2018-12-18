package(default_visibility = ["//visibility:private"])

java_library(
  name = "utils",
  srcs = glob([
    "utils/src/java/**/*.java",
  ]),
  deps = [
    "//contest",
  ],
)

java_test(
  name = "domjudge_modelobserver",
  size="small",
  test_class = "org.domjudge.api.ModelObserverTest",
  srcs = glob([
    "domjudge/src/java/org/domjudge/api/ModelObserverTest.java",
    "domjudge/src/java/org/domjudge/api/SubmitInfo.java",
  ]),
  deps = [
    ":utils",
    "//contest",
    "@mockito//jar",
    "@truth//jar",
  ],
)

java_test(
  name = "clics_parser",
  size="small",
  test_class = "edu.clics.api.RestTest",
  srcs = glob([
    "clics/src/java/edu/clics/api/RestTest.java",
  ]),
  deps = [
    "//api/clics",
    "//api/clics/proto:java",
    "@com_google_protobuf//:protobuf_java",
    "@gson_maven//jar",
    "@mockito//jar",
    "@okhttp//jar",
    "@okhttp_mockwebserver//jar",
    "@truth//jar",
  ],
)

java_test(
  name = "contest_data_structures",
  size="small",
  test_class = "me.hex539.contest.DataStructuresTest",
  srcs = glob([
    "contest/src/java/me/hex539/contest/DataStructuresTest.java",
  ]),
  deps = [
    "//contest",
    "@mockito//jar",
    "@truth//jar",
  ],
)
