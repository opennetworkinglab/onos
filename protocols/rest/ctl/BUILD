COMPILE_DEPS = CORE_DEPS + [
    "@jersey_client//jar",
    "@jersey_server//jar",
    "@jersey_common//jar",
    "@jersey_security//jar",
    "@httpclient_osgi//jar",
    "@httpcore_osgi//jar",
    "@javax_ws_rs_api//jar",
    "@hk2_api//jar",
    "@aopalliance_repackaged//jar",
    "@javax_inject//jar",
    "//protocols/rest/api:onos-protocols-rest-api",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)
