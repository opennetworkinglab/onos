COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "//incubator/api:onos-incubator-api",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Monitoring",
    description = "Shows more link properties",
    title = "Link Properties",
    url = "https://onosproject.org/",
)
