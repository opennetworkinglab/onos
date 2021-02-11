COMPILE_DEPS = CORE_DEPS + JACKSON + METRICS + KRYO + [
    "//core/common:onos-core-common",
    "//core/store/serializers:onos-core-serializers",
    "//core/store/primitives:onos-core-primitives",
    "@org_osgi_service_cm//jar",
]

TEST_DEPS = TEST + TEST_REST + TEST_ADAPTERS + [
    "//core/store/dist:onos-core-dist",
    "//core/store/dist:onos-core-dist-tests",
    "//utils/osgi:onlab-osgi-tests",
    "//pipelines/basic:onos-pipelines-basic",
    "//protocols/p4runtime/model:onos-protocols-p4runtime-model-native",
    "@minimal_json//jar",
]

osgi_jar_with_tests(
    name = "onos-core-net",
    exclude_tests = [
        "org.onosproject.net.intent.impl.compiler.AbstractLinkCollectionTest",
        "org.onosproject.net.intent.impl.installer.AbstractIntentInstallerTest",
        # FIXME: re-enable CoreEventDispatcherTest
        # Failing on Jenkins after switching to Bazel remote JDK 11
        "org.onosproject.event.impl.CoreEventDispatcherTest",
    ],
    medium_tests = ["//core/net:src/test/java/org/onosproject/net/flowobjective/impl/InOrderFlowObjectiveManagerTest"],
    test_deps = TEST_DEPS,
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)
