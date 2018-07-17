COMPILE_DEPS = CORE_DEPS + KRYO + JACKSON + CLI + [
    "//core/store/serializers:onos-core-serializers",
    "//core/store/primitives:onos-core-primitives",
    "//drivers/default:onos-drivers-default",
    "//apps/segmentrouting/app:onos-apps-segmentrouting-app",
    "//apps/route-service/api:onos-apps-route-service-api",
    "//apps/mcast/api:onos-apps-mcast-api",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//utils/misc:onlab-misc",
    "//apps/route-service/api:onos-apps-route-service-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
