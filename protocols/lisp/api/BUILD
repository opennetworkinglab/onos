COMPILE_DEPS = CORE_DEPS + NETTY + [
    "@io_netty_netty_transport//jar",
    "//protocols/lisp/msg:onos-protocols-lisp-msg",
    "//apps/mappingmanagement/api:onos-apps-mappingmanagement-api",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)
