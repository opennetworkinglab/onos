COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + [
    "@javax_ws_rs_api//jar",
    "//utils/rest:onlab-rest",
    "//core/store/serializers:onos-core-serializers",
]

TEST_DEPS = TEST_REST + [
    "@jersey_server//jar",
    "@minimal_json//jar",
    "//web/api:onos-rest-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    web_context = "/onos/v1/acl",
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Security",
    description = "ONOS ACL application.",
    title = "Access Control Lists",
    url = "http://onosproject.org",
)
