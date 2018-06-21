COMPILE_DEPS = CORE_DEPS + NETTY + [
    "@org_apache_karaf_shell_console//jar",
    "//cli:onos-cli",
]

osgi_jar_with_tests(
    import_packages = "*,org.onosproject.cli.net",
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
