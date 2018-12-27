package(default_visibility = ["//visibility:private"])

java_library(
  name = "utils",
  srcs = glob([
    "me/hex539/testing/utils/*.java",
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
    "org/domjudge/api/ModelObserverTest.java",
    "org/domjudge/api/SubmitInfo.java",
  ]),
  deps = [
    ":utils",
    "//contest",
    "@mockito//jar",
    "@truth//jar",
  ],
)

java_test(
  name = "domjudge_parser",
  size="small",
  test_class = "org.domjudge.api.RestTest",
  srcs = glob([
    "org/domjudge/api/RestTest.java",
  ]),
  deps = [
    ":utils",
    "//api/domjudge",
    "//api/domjudge/proto:java",
    "@com_google_protobuf//:protobuf_java",
    "@mockito//jar",
    "@okhttp_mockwebserver//jar",
    "@truth//jar",
  ],
)

java_test(
  name = "clics_parser",
  size="small",
  test_class = "edu.clics.api.RestTest",
  srcs = glob([
    "edu/clics/api/RestTest.java",
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
    "me/hex539/contest/DataStructuresTest.java",
  ]),
  deps = [
    "//contest",
    "@mockito//jar",
    "@truth//jar",
  ],
)

java_test(
  name = "contest_resolver",
  size="small",
  test_class = "me.hex539.contest.ResolverTest",
  srcs = glob([
    "me/hex539/contest/ResolverTest.java",
  ]),
  resources = glob([
    "resources/**",
  ]),
  deps = [
    "//contest",
    "//api/clics/proto:java",
    "@mockito//jar",
    "@truth//jar",
  ],
)
