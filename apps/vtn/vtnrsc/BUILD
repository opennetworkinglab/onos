COMPILE_DEPS = CORE_DEPS + KRYO + CLI + [
    "//core/store/serializers:onos-core-serializers",
]

osgi_jar_with_tests(
    exclude_tests = [
        "org/onosproject/vtnrsc/util/VtnEventuallyConsistentMapTest",
        "org/onosproject/vtnrsc/util/VtnStorageServiceTest",
    ],
    karaf_command_packages = [
        "org.onosproject.vtnrsc.cli",
        "org.onosproject.vtnrsc.cli.virtualport",
        "org.onosproject.vtnrsc.cli.subnet",
        "org.onosproject.vtnrsc.cli.routerinterface",
        "org.onosproject.vtnrsc.cli.router",
        "org.onosproject.vtnrsc.cli.network",
        "org.onosproject.vtnrsc.cli.floatingip",
    ],
    test_deps = TEST_REST,
    deps = COMPILE_DEPS,
)
