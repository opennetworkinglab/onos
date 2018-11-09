COMPILE_DEPS = CORE_DEPS + CLI + [
    "//protocols/pcep/server/ctl:onos-protocols-pcep-server-ctl",
    "//protocols/pcep/server/api:onos-protocols-pcep-server-api",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.pcep.cli"],
    deps = COMPILE_DEPS,
)
