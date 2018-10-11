COMPILE_DEPS = CORE_DEPS + JACKSON + CLI + [
    "@ganymed_ssh2//jar",
    "@sshd_core//jar",
    "@bcpkix_jdk15on//jar",
    "@bcprov_jdk15on//jar",
    "//protocols/netconf/api:onos-protocols-netconf-api",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//utils/osgi:onlab-osgi-tests",
    "//core/api:onos-api-tests",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.netconf.cli.impl"],
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
