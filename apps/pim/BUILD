COMPILE_DEPS = CORE_DEPS + JACKSON + CLI + [
    "//incubator/api:onos-incubator-api",
    "//apps/routing-api:onos-apps-routing-api",
    "//apps/route-service/api:onos-apps-route-service-api",
]

BUNDLES = [
    "//apps/routing-api:onos-apps-routing-api",
    ":onos-apps-pim",
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Traffic Engineering",
    description = "Protocol independent multicast emulation.",
    included_bundles = BUNDLES,
    required_apps = ["org.onosproject.route-service"],
    title = "Protocol Independent Multicast Emulation",
    url = "http://onosproject.org",
)
