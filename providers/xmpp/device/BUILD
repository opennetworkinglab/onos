COMPILE_DEPS = CORE_DEPS + [
    "@tinder_xmpp//jar",
    "//protocols/xmpp/core/api:onos-protocols-xmpp-core-api",
    "//protocols/xmpp/core/ctl:onos-protocols-xmpp-core-ctl",
]

osgi_jar_with_tests(
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.xmpp.device",
    category = "Provider",
    description = "XMPP protocol southbound provider.",
    required_apps = [
        "org.onosproject.protocols.xmpp",
    ],
    title = "XMPP Device Provider",
    url = "https://wiki.onosproject.org/display/ONOS/XMPP+as+SBI",
)
