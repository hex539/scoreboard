package(default_visibility = ["//visibility:private"])

java_library(
    name = "utils",
    srcs = glob([
        "me/hex539/testing/utils/*.java",
    ]),
    deps = [
        "//contest:immutable",
        "//contest:model",
        "@com_google_protobuf//:protobuf_java",
    ],
)

java_test(
    name = "domjudge_modelobserver",
    size = "small",
    srcs = glob([
        "org/domjudge/api/ModelObserverTest.java",
        "org/domjudge/api/SubmitInfo.java",
    ]),
    test_class = "org.domjudge.api.ModelObserverTest",
    deps = [
        ":utils",
        "//contest:judgement_dispatcher",
        "//contest:model",
        "//contest:mutable",
        "@com_google_protobuf//:protobuf_java",
        "@maven//:com_google_truth_truth",
        "@maven//:org_mockito_mockito_all",
    ],
)

java_test(
    name = "domjudge_parser",
    size = "small",
    srcs = glob([
        "org/domjudge/api/RestTest.java",
    ]),
    test_class = "org.domjudge.api.RestTest",
    deps = [
        ":utils",
        "//api/domjudge",
        "//api/domjudge/proto:java",
        "@com_google_protobuf//:protobuf_java",
        "@maven//:com_google_truth_truth",
        "@maven//:com_squareup_okhttp3_mockwebserver",
        "@maven//:org_mockito_mockito_all",
    ],
)

java_test(
    name = "clics_parser",
    size = "small",
    srcs = glob([
        "edu/clics/api/RestTest.java",
    ]),
    test_class = "edu.clics.api.RestTest",
    deps = [
        "//api/clics",
        "//api/clics/proto:java",
        "@com_google_protobuf//:protobuf_java",
        "@maven//:com_google_code_gson_gson",
        "@maven//:com_google_truth_truth",
        "@maven//:com_squareup_okhttp3_mockwebserver",
        "@maven//:com_squareup_okhttp3_okhttp",
        "@maven//:org_mockito_mockito_all",
    ],
)

java_test(
    name = "contest_data_structures",
    size = "small",
    srcs = glob([
        "me/hex539/contest/DataStructuresTest.java",
    ]),
    test_class = "me.hex539.contest.DataStructuresTest",
    deps = [
        "//contest:data_structures",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_truth_truth",
        "@maven//:org_mockito_mockito_all",
    ],
)

java_test(
    name = "contest_resolver",
    size = "small",
    srcs = glob([
        "me/hex539/contest/ResolverTest.java",
    ]),
    resources = glob([
        "resources/**",
    ]),
    test_class = "me.hex539.contest.ResolverTest",
    deps = [
        "//api/clics/proto:java",
        "//contest:immutable",
        "//contest:model",
        "//contest:mutable",
        "//contest:network",
        "//contest:resolver",
        "@com_google_protobuf//:protobuf_java_util",
        "@maven//:com_google_truth_truth",
        "@maven//:org_mockito_mockito_all",
    ],
)
