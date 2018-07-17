COMPILE_DEPS = CORE_DEPS + CLI + JACKSON

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    deps = COMPILE_DEPS,
)

onos_app(
    category = "GUI",
    description = "Extends the ONOS GUI with a view that allows the operator to visualize " +
                  "the currently loaded drivers.",
    title = "Driver Support Matrix",
    url = "http://onosproject.org",
)
