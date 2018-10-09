COMPILE_DEPS = CORE_DEPS + CLI

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.intentsync.cli"],
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.intentsynchronizer",
    category = "Utility",
    description = "Synchronizes intents to the intent framework from a single instance",
    title = "Intent Synchronizer",
    url = "http://onosproject.org",
)
