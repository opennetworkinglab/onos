COMPILE_DEPS = CORE_DEPS + NETTY + JACKSON + [
    "@io_netty_netty//jar",
    "//protocols/isis/api:onos-protocols-isis-api",
    "//protocols/isis/isisio:onos-protocols-isis-isisio",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)
