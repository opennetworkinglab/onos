COMPILE_DEPS = CORE_DEPS + KRYO + JACKSON + [
    "//core/store/dist:onos-core-dist",
    "//core/store/serializers:onos-core-serializers",
]

osgi_jar_with_tests(
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)
