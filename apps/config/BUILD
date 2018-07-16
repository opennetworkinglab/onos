APPS = [
    "org.onosproject.yang",
]

COMPILE_DEPS = CORE_DEPS + KRYO + CLI + ONOS_YANG + [
    "//core/store/serializers:onos-core-serializers",
    "@commons_text//jar",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Utility",
    description = "Provides means to track and distribute service and device configuration data " +
                  "throughout the ONOS cluster. It works with the YANG runtime to ensure that the tracked " +
                  "data adheres to the registered YANG models. Applications can traverse and access this " +
                  "data which is organized in a logically unified tree.<p/>(Under development)",
    required_apps = APPS,
    title = "Dynamic Configuration",
    url = "http://onosproject.org",
)
