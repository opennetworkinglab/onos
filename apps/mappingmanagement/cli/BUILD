COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "@org_apache_karaf_shell_console//jar",
    "@javax_ws_rs_api//jar",
    "//incubator/api:onos-incubator-api",
    "//cli:onos-cli",
    "//utils/rest:onlab-rest",
    "//apps/mappingmanagement/api:onos-apps-mappingmanagement-api",
    "//core/store/serializers:onos-core-serializers",
]

osgi_jar_with_tests(
    import_packages = "*,org.onosproject.cli.net",
    deps = COMPILE_DEPS,
)
