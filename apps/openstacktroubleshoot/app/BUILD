COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + CLI + REST + [
    "//apps/openstacktroubleshoot/api:onos-apps-openstacktroubleshoot-api",
]

TEST_DEPS = TEST_ADAPTERS + TEST_REST + [
    "//core/api:onos-api-tests",
    "//core/common:onos-core-common-tests",
    "//web/api:onos-rest-tests",
]

osgi_jar_with_tests(
    api_description = "REST API for OpenStack Troubleshoot",
    api_package = "org.onosproject.openstacktroubleshoot.web",
    api_title = "OpenStack Troubleshoot API",
    api_version = "1.0",
    test_deps = TEST_DEPS,
    web_context = "/onos/openstacktroubleshoot",
    deps = COMPILE_DEPS,
)
