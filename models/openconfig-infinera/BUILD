load("//tools/build/bazel:yang.bzl", "yang_model")

COMPILE_DEPS = [
    "//models/ietf:onos-models-ietf",
]

APPS = [
    "org.onosproject.models.ietf",
]

yang_model(
    app_name = "org.onosproject.models.openconfig-infinera",
    custom_registrator = False,
    required_apps = APPS,
    title = "OpenConfig Infinera XT3300 YANG Models",
    deps = COMPILE_DEPS,
)
