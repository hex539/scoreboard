package(default_visibility = ["//visibility:public"])

java_library(
  name = "utils",
  srcs = glob([
    "utils/src/java/**/*.java",
  ]),
  deps = [
    "//domjudge:api",
    "//domjudge:proto",
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
    "//domjudge:api",
    "//domjudge:proto",
    "@mockito//jar",
    "@truth//jar",
  ],
)
