COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "@ganymed_ssh2//jar",
    "@org_apache_karaf_shell_console//jar",
    "@sshd_core//jar",
    "@bcpkix_jdk15on//jar",
    "@bcprov_jdk15on//jar",
    "//protocols/netconf/api:onos-protocols-netconf-api",
    "//cli:onos-cli",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//utils/osgi:onlab-osgi-tests",
    "//core/api:onos-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
