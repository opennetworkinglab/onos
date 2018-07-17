COMPILE_DEPS = CORE_DEPS + JACKSON + ONOS_YANG + [
    "@onos_yang_runtime//jar",
    "@javax_ws_rs_api//jar",
    "//utils/rest:onlab-rest",
    "//apps/restconf/api:onos-apps-restconf-api",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)
