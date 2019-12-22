package(default_visibility = ["//visibility:private"])

android_library(
    name = "code",
    srcs = glob([
        "src/java/**",
    ]),
    assets = glob([
        "assets/**",
    ]),
    assets_dir = "assets",
    custom_package = "me.hex539.app",
    manifest = "AndroidManifest.xml",
    resource_files = glob([
        "res/**",
    ]),
    deps = [
        "//contest",
        "//contest:data_structures",
        "//external:guava",
        "//scoreboards/android/proto:java",
        "//third_party/auto:auto_value",
        "//third_party/auto:auto_value_annotations",
        "@maven//:com_android_support_cardview_v7",
        "@maven//:com_android_support_leanback_v17",
        "@maven//:com_android_support_preference_leanback_v17",
        "@maven//:com_android_support_preference_v7",
        "@maven//:com_android_support_recyclerview_v7",
        "@maven//:com_android_support_support_annotations",
        "@maven//:com_android_support_support_compat",
        "@maven//:com_android_support_support_fragment",
    ],
)

android_binary(
    name = "android",
    custom_package = "me.hex539.app",
    dex_shards = 25,
    dexopts = [
        "--min-sdk-version=26",
    ],
    incremental_dexing = 0,
    manifest = "AndroidManifest.xml",
    manifest_values = {
        "minSdkVersion": "26",
        "targetSdkVersion": "26",
    },
    multidex = "native",
    deps = [
        ":code",
        "//contest",
    ],
)
