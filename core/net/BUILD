COMPILE_DEPS = CORE_DEPS + JACKSON + METRICS + KRYO + [
    "//core/common:onos-core-common",
    "//utils/rest:onlab-rest",
    "//core/store/serializers:onos-core-serializers",
    "//core/store/primitives:onos-core-primitives",
    "@org_osgi_service_cm//jar",
]

TEST_DEPS = TEST + TEST_REST + TEST_ADAPTERS + [
    "//core/store/dist:onos-core-dist",
    "//core/store/dist:onos-core-dist-tests",
    "//utils/osgi:onlab-osgi-tests",
    "//pipelines/basic:onos-pipelines-basic",
    "@minimal_json//jar",
]

osgi_jar_with_tests(
    name = "onos-core-net",
    exclude_tests = [
        "org.onosproject.net.intent.impl.compiler.AbstractLinkCollectionTest",
        "org.onosproject.net.intent.impl.installer.AbstractIntentInstallerTest",
    ],
    medium_tests = ["//core/net:src/test/java/org/onosproject/net/flowobjective/impl/InOrderFlowObjectiveManagerTest"],
    test_deps = TEST_DEPS,
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)
