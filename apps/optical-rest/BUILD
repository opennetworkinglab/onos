COMPILE_DEPS = CORE_DEPS + JACKSON + REST + CLI + [
    "//apps/optical-model:onos-apps-optical-model",
]

APPS = [
    "org.onosproject.optical-model",
]

osgi_jar_with_tests(
    api_description = "REST API for Optical Model",
    api_package = "org.onosproject.net.optical.rest",
    api_title = "Optical Network Model REST API",
    api_version = "1.0",
    web_context = "/onos/optical",
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Optical",
    description = "This application provides REST support for optical network model.",
    required_apps = APPS,
    title = "Optical Network Model REST API",
    url = "http://onosproject.org",
)
