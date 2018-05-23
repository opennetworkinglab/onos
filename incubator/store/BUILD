COMPILE_DEPS = CORE_DEPS + KRYO + [
    "//core/common:onos-core-common",
    "//utils/rest:onlab-rest",
    "//core/store/serializers:onos-core-serializers",
    "@concurrent_trees//jar",
    "//incubator/api:onos-incubator-api",
]

TEST_DEPS = TEST_ADAPTERS

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)
