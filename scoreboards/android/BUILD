package(default_visibility = ["//visibility:private"])

android_binary(
  name = "android",
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
  assets_dir = "assets",
  assets = glob([
    "assets/**",
  ]),
  resource_files = glob([
    "res/**",
  ]),
  deps = [
    "//clics/proto",
    "//contest",
    "@androidsdk//com.android.support:recyclerview-v7-25.3.1",
    "@androidsdk//com.android.support:cardview-v7-25.3.1",
    "@auto_value//:processor",
  ],
)
