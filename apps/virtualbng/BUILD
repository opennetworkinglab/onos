COMPILE_DEPS = CORE_DEPS + JACKSON + CLI + REST

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.virtualbng.cli"],
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Traffic Engineering",
    description = "A virtual Broadband Network Gateway(BNG) application.",
    title = "Virtual Broadband Gateway",
    url = "http://onosproject.org",
)
