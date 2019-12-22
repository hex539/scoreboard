package(default_visibility = ["//visibility:private"])

py_binary(
    name = "replay",
    srcs = glob([
        "*.py",
    ]),
    data = [
        "templates",
    ],
    python_version = "PY2",
    deps = [
        "//api/clics/proto:python",
    ],
)
