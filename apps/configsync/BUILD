APPS = [
    # dynamic config
    "org.onosproject.config",
]

COMPILE_DEPS = CORE_DEPS + ONOS_YANG + [
    "//apps/config:onos-apps-config",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Utility",
    description = "Application to support the Dynamic configuration service.",
    required_apps = APPS,
    title = "Dynamic Configuration Synchronizer",
    url = "http://onosproject.org",
)
