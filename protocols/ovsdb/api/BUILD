COMPILE_DEPS = CORE_DEPS + NETTY + JACKSON + [
    "@netty_transport//jar",
    "//protocols/ovsdb/rfc:onos-protocols-ovsdb-rfc",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)
