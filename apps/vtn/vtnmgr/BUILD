COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + CLI + [
    "//core/store/serializers:onos-core-serializers",
    "//apps/vtn/vtnrsc:onos-apps-vtn-vtnrsc",
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    deps = COMPILE_DEPS,
)
