COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + [
    "@org_apache_karaf_shell_console//jar",
    "@javax_ws_rs_api//jar",
    "//cli:onos-cli",
    "//core/store/serializers:onos-core-serializers",
    "//utils/rest:onlab-rest",
    "//incubator/api:onos-incubator-api",
    "//apps/pcep-api:onos-apps-pcep-api",
    "//apps/pce/bandwidthmgmt:onos-apps-pce-bandwidthmgmt",
]

osgi_jar_with_tests(
    test_deps = TEST_REST,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.pce.app",
    category = "default",
    description = "PCE as centeral controller App.",
    title = "PCE",
    url = "http://onosproject.org",
)
