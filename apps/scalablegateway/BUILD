COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + CLI + [
    "//core/store/serializers:onos-core-serializers",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.scalablegateway.cli"],
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Traffic Engineering",
    description = "Scalable gateway service",
    title = "Scalable Gateway",
    url = "http://onosproject.org",
)
