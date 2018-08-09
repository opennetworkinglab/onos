COMPILE_DEPS = CORE_DEPS + NETTY + JACKSON + [
    "@io_netty_netty_transport//jar",
    "//protocols/ovsdb/rfc:onos-protocols-ovsdb-rfc",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)
