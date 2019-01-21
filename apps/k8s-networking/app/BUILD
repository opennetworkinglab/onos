COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + CLI + REST + [
    "//core/store/serializers:onos-core-serializers",
    "//apps/k8s-node/api:onos-apps-k8s-node-api",
    "//apps/k8s-networking/api:onos-apps-k8s-networking-api",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//core/api:onos-api-tests",
    "//core/common:onos-core-common-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
