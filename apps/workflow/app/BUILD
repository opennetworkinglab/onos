COMPILE_DEPS = CORE_DEPS + KRYO + JACKSON + CLI + REST + [
    "@jersey_server//jar",
    "//core/store/serializers:onos-core-serializers",
    "//core/store/primitives:onos-core-primitives",
    "//apps/workflow/api:onos-apps-workflow-api",
]

osgi_jar_with_tests(
    api_description = "REST API for workflow",
    api_package = "org.onosproject.workflow.rest",
    api_title = "Workflow REST API",
    api_version = "1.0",
    karaf_command_packages = ["org.onosproject.workflow.cli"],
    test_deps = TEST_ADAPTERS,
    web_context = "/onos/workflow",
    deps = COMPILE_DEPS,
)
