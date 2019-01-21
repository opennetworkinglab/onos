COMPILE_DEPS = CORE_DEPS + KRYO + JACKSON + CLI + [
    "//core/store/serializers:onos-core-serializers",
    "//apps/workflow/api:onos-apps-workflow-api",
]

osgi_jar(
    karaf_command_packages = ["org.onosproject.workflow.cli"],
    deps = COMPILE_DEPS,
)
