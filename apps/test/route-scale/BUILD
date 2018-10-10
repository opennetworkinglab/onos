COMPILE_DEPS = CORE_DEPS + CLI + REST + [
    "//apps/route-service/api:onos-apps-route-service-api",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.routescale"],
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.routescale",
    category = "Test Utility",
    description = "Route and flow scalability test facility.",
    required_apps = ["org.onosproject.route-service"],
    title = "Route and Flow Scalability Test",
    url = "http://onosproject.org",
)
