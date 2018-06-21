COMPILE_DEPS = CORE_DEPS + NETTY + JACKSON + [
    "@netty_transport//jar",
    "@netty_codec//jar",
    "@netty_handler//jar",
    "//protocols/ovsdb/rfc:onos-protocols-ovsdb-rfc",
    "//protocols/ovsdb/api:onos-protocols-ovsdb-api",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)
