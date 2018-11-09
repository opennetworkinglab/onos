COMPILE_DEPS = CORE_DEPS + ONOS_YANG + JACKSON + [
    "@jersey_server//jar",
    "@javax_ws_rs_api//jar",
    "//core/store/serializers:onos-core-serializers",
    "//apps/restconf/api:onos-apps-restconf-api",
    "//apps/restconf/utils:onos-apps-restconf-utils",
    "//apps/config:onos-apps-config",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)
