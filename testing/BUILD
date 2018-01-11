package(default_visibility = ["//visibility:public"])

java_library(
  name = "utils",
  srcs = glob([
    "utils/src/java/**/*.java",
  ]),
  deps = [
    "//clics/proto",
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
    "//clics/proto",
    "//contest",
    "@mockito//jar",
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
