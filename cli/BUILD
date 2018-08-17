COMPILE_DEPS = CORE_DEPS + JACKSON + METRICS + [
    "@org_apache_karaf_shell_core//jar",
    "//incubator/api:onos-incubator-api",
    "//incubator/net:onos-incubator-net",
    "//utils/rest:onlab-rest",
    "//core/common:onos-core-common",
]

osgi_jar(
    karaf_command_packages = [
        "org.onosproject.cli",
        "org.onosproject.cli.app",
        "org.onosproject.cli.cfg",
        "org.onosproject.cli.net",
        "org.onosproject.cli.security",
    ],
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)
