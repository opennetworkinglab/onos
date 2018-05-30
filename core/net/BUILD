COMPILE_DEPS = CORE_DEPS + JACKSON + METRICS + KRYO + [
    "//core/common:onos-core-common",
    "//incubator/api:onos-incubator-api",
    "//utils/rest:onlab-rest",
    "//incubator/net:onos-incubator-net",
    "//incubator/store:onos-incubator-store",
    "//core/store/serializers:onos-core-serializers",
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
    test_deps = TEST_DEPS,
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)
