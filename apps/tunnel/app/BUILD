COMPILE_DEPS = CORE_DEPS + JACKSON + REST + CLI + [
    "//core/store/serializers:onos-core-serializers",
    "//apps/tunnel/api:onos-apps-tunnel-api",
]

TEST_DEPS = TEST_ADAPTERS

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)
