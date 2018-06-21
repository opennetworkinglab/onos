COMPILE_DEPS = CORE_DEPS + [
    "@org_apache_karaf_shell_console//jar",
    "//incubator/api:onos-incubator-api",
    "//protocols/pcep/server/ctl:onos-protocols-pcep-server-ctl",
    "//protocols/pcep/server/api:onos-protocols-pcep-server-api",
    "//cli:onos-cli",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)
