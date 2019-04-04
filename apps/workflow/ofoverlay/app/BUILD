COMPILE_DEPS = CORE_DEPS + KRYO + JACKSON + CLI + [
    "//core/store/serializers:onos-core-serializers",
    "//core/store/primitives:onos-core-primitives",
    "//apps/workflow/api:onos-apps-workflow-api",
    "//apps/workflow/model:onos-apps-workflow-model",
    "@jsch//jar",
    "//protocols/ovsdb/api:onos-protocols-ovsdb-api",
    "//protocols/ovsdb/rfc:onos-protocols-ovsdb-rfc",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)
