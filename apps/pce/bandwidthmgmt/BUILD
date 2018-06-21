COMPILE_DEPS = CORE_DEPS + KRYO + [
    "//core/store/serializers:onos-core-serializers",
    "//apps/pcep-api:onos-apps-pcep-api",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.bandwidthmgmt",
    category = "default",
    description = "PCE Bandwidth Management.",
    title = "PCE Bandwidth Management",
    url = "http://onosproject.org",
)
