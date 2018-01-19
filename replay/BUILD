package(default_visibility = ["//visibility:private"])

py_binary(
  name = "replay",
  srcs = glob([
    "*.py",
  ]),
  data = [
    "templates",
  ],
  deps = [
    "//clics/proto:python",
  ],
)
