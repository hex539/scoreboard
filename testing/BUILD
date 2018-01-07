package(default_visibility = ["//visibility:public"])

java_library(
  name = "utils",
  srcs = glob([
    "utils/src/java/**/*.java",
  ]),
  deps = [
    "//domjudge/proto",
    "//domjudge/scoreboard",
  ],
)

java_test(
  name = "domjudge_modelobserver",
  size="small",
  test_class = "org.domjudge.api.ModelObserverTest",
  srcs = glob([
    "domjudge/src/java/org/domjudge/api/ModelObserverTest.java",
  ]),
  deps = [
    ":utils",
    "//domjudge/proto",
    "//domjudge/scoreboard",
    "@mockito//jar",
    "@truth//jar",
  ],
)
