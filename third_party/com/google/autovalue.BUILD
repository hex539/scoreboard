java_import(
    name = "jar",
    jars = ["auto-value-1.4.jar"],
)

java_plugin(
    name = "autovalue-plugin",
    generates_api = 1,
    processor_class = "com.google.auto.value.processor.AutoValueProcessor",
    deps = [":jar"],
)

java_library(
    name = "processor",
    exported_plugins = [":autovalue-plugin"],
    exports = [":jar"],
    visibility = ["//visibility:public"],
)
