COMPILE_DEPS = CORE_DEPS + JACKSON + METRICS + [
    "@org_apache_karaf_shell_console//jar",
    "//incubator/api:onos-incubator-api",
    "//incubator/net:onos-incubator-net",
    "//utils/rest:onlab-rest",
    "//core/common:onos-core-common",
]

osgi_jar(
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)
