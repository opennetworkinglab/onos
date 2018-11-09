COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "@javax_ws_rs_api//jar",
    #"//protocols/ovsdb/api:onos-protocols-ovsdb-api",
    #"//protocols/ovsdb/rfc:onos-protocols-ovsdb-rfc",
    #"//apps/pcep-api:onos-apps-pcep-api",
    "//protocols/rest/api:onos-protocols-rest-api",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)
