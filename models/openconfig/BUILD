load("//tools/build/bazel:yang.bzl", "yang_model")

COMPILE_DEPS = [
    "//models/ietf:onos-models-ietf",
]

APPS = [
    "org.onosproject.models.ietf",
]

yang_model(
    app_name = "org.onosproject.models.openconfig",
    custom_registrator = True,
    required_apps = APPS,
    title = "OpenConfig YANG Models",
    deps = COMPILE_DEPS,
)
