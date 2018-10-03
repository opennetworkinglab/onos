COMPILE_DEPS = CORE_DEPS + CLI + [
    "//incubator/api:onos-incubator-api",
    "//apps/faultmanagement/fmmgr:onos-apps-faultmanagement-fmmgr",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.faultmanagement.alarms.cli"],
    deps = COMPILE_DEPS,
)
