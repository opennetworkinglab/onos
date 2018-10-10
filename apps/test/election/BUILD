COMPILE_DEPS = CORE_DEPS + CLI + REST

osgi_jar(
    karaf_command_packages = ["org.onosproject.election.cli"],
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Test Utility",
    description = "Master election test application.",
    title = "Master Election Test",
    url = "http://onosproject.org",
)
