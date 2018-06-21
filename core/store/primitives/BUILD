COMPILE_DEPS = CORE_DEPS + KRYO + [
    "@atomix//jar",
    "//core/common:onos-core-common",
    "//core/store/serializers:onos-core-serializers",
    "//incubator/api:onos-incubator-api",
]

TEST_DEPS = TEST + [
    "@commons_math3//jar",
    "@netty_handler//jar",
    "@netty_buffer//jar",
    "@netty_codec//jar",
    "@netty_resolver//jar",
    "//core/api:onos-api-tests",
]

osgi_jar_with_tests(
    name = "onos-core-primitives",
    test_deps = TEST_DEPS,
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)
