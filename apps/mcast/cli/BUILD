COMPILE_DEPS = CORE_DEPS + JACKSON + CLI + REST + [
    "//core/store/serializers:onos-core-serializers",
    "//apps/mcast/api:onos-apps-mcast-api",
]

osgi_jar(
    karaf_command_packages = ["org.onosproject.mcast.cli"],
    deps = COMPILE_DEPS,
)
