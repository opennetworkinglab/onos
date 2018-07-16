load("//tools/build/bazel:yang.bzl", "yang_model")

COMPILE_DEPS = [
    "//models/common:onos-models-common",
]

APPS = [
    "org.onosproject.models.common",
]

yang_model(
    app_name = "org.onosproject.models.polatis",
    required_apps = APPS,
    title = "Polatis YANG Model",
    deps = COMPILE_DEPS,
)
