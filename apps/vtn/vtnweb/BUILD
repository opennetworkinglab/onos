COMPILE_DEPS = CORE_DEPS + JACKSON + REST + [
    "//apps/vtn/vtnrsc:onos-apps-vtn-vtnrsc",
]

TEST_DEPS = TEST_REST + [
    "@minimal_json//jar",
    "//utils/osgi:onlab-osgi-tests",
    "//web/api:onos-rest-tests",
]

osgi_jar_with_tests(
    exclude_tests = ["org.onosproject.vtnweb.resources.VtnResourceTest"],
    test_deps = TEST_DEPS,
    web_context = "/onos/vtn",
    deps = COMPILE_DEPS,
)
