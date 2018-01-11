package(default_visibility = ["//visibility:private"])

android_binary(
  name = "app",
  manifest = "AndroidManifest.xml",
  custom_package = "me.hex539.app",
  multidex = "native",
  incremental_dexing = 0,
  dex_shards = 25,
  manifest_values = {
    "minSdkVersion": "26",
    "targetSdkVersion": "26",
  },
  dexopts = [
    "--min-sdk-version=26",
  ],
  srcs = glob([
    "src/java/**",
  ]),
  resource_files = glob([
    "res/**",
  ]),
  deps = [
    "//domjudge/api:android",
    "//domjudge/proto",
    "//domjudge/scoreboard:android",
    "@androidsdk//com.android.support:recyclerview-v7-25.3.1",
  ],
)
