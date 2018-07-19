COMPILE_DEPS = CORE_DEPS + JACKSON + REST + ONOS_YANG + [
    "@jersey_media_multipart//jar",
    "@onos_yang_runtime//jar",
    "//apps/yang:onos-apps-yang",
]

osgi_jar_with_tests(
    api_description = "REST API for YANG Runtime",
    api_package = "org.onosproject.yang.web",
    api_title = "YANG Runtime",
    api_version = "1.0",
    suppress_javadocs = True,
    test_deps = TEST_ADAPTERS,
    web_context = "/onos/yang",
    deps = COMPILE_DEPS,
)
