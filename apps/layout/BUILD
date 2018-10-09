COMPILE_DEPS = CORE_DEPS + JACKSON + CLI + [
    "//core/common:onos-core-common",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.layout"],
    deps = COMPILE_DEPS,
)

onos_app(
    category = "GUI",
    description = "Automatically lays out the network topology using roles assigned to each " +
                  "network element via the network configuration. Supports multiple layout variants.",
    title = "UI Auto-Layout",
    url = "http://onosproject.org",
)
