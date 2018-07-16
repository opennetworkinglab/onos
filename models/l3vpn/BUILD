load("//tools/build/bazel:yang.bzl", "yang_model")

COMPILE_DEPS = [
    "//models/common:onos-models-common",
]

APPS = [
    "org.onosproject.models.common",
]

yang_model(
    app_name = "org.onosproject.models.l3vpn",
    custom_registrator = True,
    required_apps = APPS,
    title = "L3VPN YANG Models",
    deps = COMPILE_DEPS,
)
