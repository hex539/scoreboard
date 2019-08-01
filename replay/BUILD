package(default_visibility = ["//visibility:private"])

py_binary(
  name = "replay",
  python_version = "PY2",
  srcs = glob([
    "*.py",
  ]),
  data = [
    "templates",
  ],
  deps = [
    "//api/clics/proto:python",
  ],
)
