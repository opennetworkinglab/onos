COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "@org_apache_karaf_shell_console//jar",
    "//apps/pce/app:onos-apps-pce-app",
    "//cli:onos-cli",
    "//incubator/api:onos-incubator-api",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Utility",
    description = "Allows the user to visualize different types of paths between network entities",
    title = "PCE REST API",
    url = "http://onosproject.org",
)
