COMPILE_DEPS = CORE_DEPS + JACKSON + CLI + REST + [
    "//apps/cfm/api:onos-apps-cfm-api",
]

TEST_DEPS = TEST_REST + [
    "//utils/osgi:onlab-osgi-tests",
    "//web/api:onos-rest-tests",
]

osgi_jar_with_tests(
    api_description = "REST API for L2 Monitoring CFM",
    api_package = "org.onosproject.soam.rest",
    api_title = "L2 Monitoring CFM",
    api_version = "1.0",
    exclude_tests = ["org/onosproject/cfm/impl/CfmResourceTest"],
    karaf_command_packages = [
        "org.onosproject.cfm.cli",
        "org.onosproject.cfm.cli.completer",
    ],
    test_deps = TEST_DEPS,
    web_context = "/onos/cfm",
    deps = COMPILE_DEPS,
)
