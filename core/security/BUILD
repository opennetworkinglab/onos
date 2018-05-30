COMPILE_DEPS = CORE_DEPS + KRYO + [
    "@org_apache_felix_framework_security//jar",
    "//core/store/serializers:onos-core-serializers",
]

osgi_jar_with_tests(
    name = "onos-security",
    test_deps = TEST,
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)
