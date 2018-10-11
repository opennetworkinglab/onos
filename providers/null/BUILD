COMPILE_DEPS = CORE_DEPS + NETTY + CLI

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.provider.nil.cli"],
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Test Utility",
    description = "Suite of device, link, host, flow and packet providers that allow the operator " +
                  "to test ONOS against completely fictional network topology. This is useful for conducting " +
                  "ONOS core performance tests and for developing features that require only control plane " +
                  "- and not data plane - functionality. ",
    title = "Null Provider Suite",
    url = "http://onosproject.org",
)
