COMPILE_DEPS = CORE_DEPS + CLI + REST + [
    "//core/store/serializers:onos-core-serializers",
    "//apps/mcast/api:onos-apps-mcast-api",
]

osgi_jar(
    import_packages = "*,org.onosproject.cli.net",
    deps = COMPILE_DEPS,
)
