licenses(["notice"])

java_library(
    name = "auto_value",
    exported_plugins = [
        ":auto_annotation_plugin",
        ":auto_value_plugin",
    ],
    neverlink = True,
    visibility = ["//visibility:public"],
    exports = ["@maven//:com_google_auto_value_auto_value"],
)

java_library(
    name = "auto_value_annotations",
    exported_plugins = [
        ":auto_annotation_plugin",
        ":auto_value_plugin",
    ],
    visibility = ["//visibility:public"],
    exports = ["@maven//:com_google_auto_value_auto_value_annotations"],
)

java_plugin(
    name = "auto_annotation_plugin",
    processor_class = "com.google.auto.value.processor.AutoAnnotationProcessor",
    deps = [
        "@maven//:com_google_auto_value_auto_value",
        "@maven//:com_google_auto_value_auto_value_annotations",
    ],
)

java_plugin(
    name = "auto_value_plugin",
    processor_class = "com.google.auto.value.processor.AutoValueProcessor",
    deps = [
        "@maven//:com_google_auto_value_auto_value",
        "@maven//:com_google_auto_value_auto_value_annotations",
    ],
)
