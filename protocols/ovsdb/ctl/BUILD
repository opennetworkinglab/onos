COMPILE_DEPS = CORE_DEPS + NETTY + JACKSON + [
    "@io_netty_netty_transport//jar",
    "@io_netty_netty_codec//jar",
    "@io_netty_netty_handler//jar",
    "//protocols/ovsdb/rfc:onos-protocols-ovsdb-rfc",
    "//protocols/ovsdb/api:onos-protocols-ovsdb-api",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)
