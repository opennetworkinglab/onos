COMPILE_DEPS = CORE_DEPS + JACKSON + CLI

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.events"],
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Test Utility",
    description = "Allows recording and tracking of otherwise ephemeral ONOS core events. " +
                  "It is primarily aimed for supporting various system tests and to assure that expected " +
                  "events are raised and broadcast to applications that have requested them.",
    title = "Event History",
    url = "http://onosproject.org",
)
