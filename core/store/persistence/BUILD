COMPILE_DEPS = CORE_DEPS + [
    "@mapdb//jar",
]

osgi_jar_with_tests(
    name = "onos-core-persistence",
    exclude_tests = [
        "org.onosproject.persistence.impl.MapDBTest",
    ],
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)
