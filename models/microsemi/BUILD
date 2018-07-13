load("//tools/build/bazel:yang.bzl", "yang_model")

COMPILE_DEPS = [
    "//models/common:onos-models-common",
]

APPS = [
    "org.onosproject.models.common",
]

yang_model(
    app_name = "org.onosproject.models.microsemi",
    title = "Microsemi YANG Models",
    custom_registrator = True,
    deps = COMPILE_DEPS,
    required_apps = APPS,
)
