COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "//protocols/ovsdb/api:onos-protocols-ovsdb-api",
    "//protocols/ovsdb/rfc:onos-protocols-ovsdb-rfc",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Protocol",
    description = "PCEP protocol API.",
    title = "PCEP Protocol API",
    url = "http://onosproject.org",
)
