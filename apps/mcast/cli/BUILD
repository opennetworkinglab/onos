COMPILE_DEPS = CORE_DEPS + [
    "@org_apache_karaf_shell_console//jar",
    "@javax_ws_rs_api//jar",
    "//cli:onos-cli",
    "//utils/rest:onlab-rest",
    "//core/store/serializers:onos-core-serializers",
    "//apps/mcast/api:onos-apps-mcast-api",
]

osgi_jar(
    import_packages = "*,org.onosproject.cli.net",
    deps = COMPILE_DEPS,
)
