COMPILE_DEPS = CORE_DEPS + [
    "//protocols/ovsdb/api:onos-protocols-ovsdb-api",
    "//protocols/ovsdb/rfc:onos-protocols-ovsdb-rfc",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.ovsdbhostprovider",
    category = "Provider",
    description = "OVSDB Host provider.",
    required_apps = ["org.onosproject.ovsdb-base"],
    title = "OVSDB host Provider",
    url = "http://onosproject.org",
)
