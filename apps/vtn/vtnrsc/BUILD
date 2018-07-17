COMPILE_DEPS = CORE_DEPS + KRYO + CLI + [
    "//core/store/serializers:onos-core-serializers",
]

osgi_jar_with_tests(
    exclude_tests = [
        "org/onosproject/vtnrsc/util/VtnEventuallyConsistentMapTest",
        "org/onosproject/vtnrsc/util/VtnStorageServiceTest",
    ],
    test_deps = TEST_REST,
    deps = COMPILE_DEPS,
)
