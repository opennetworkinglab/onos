COMPILE_DEPS = CORE_DEPS + [
    "//apps/p4-tutorial/pipeconf:onos-apps-p4-tutorial-pipeconf",
]

osgi_jar(
    deps = COMPILE_DEPS,
)

BUNDLES = [
    "//apps/p4-tutorial/mytunnel:onos-apps-p4-tutorial-mytunnel",
]

onos_app(
    app_name = "org.onosproject.p4tutorial.mytunnel",
    category = "Traffic Engineering",
    description = "Provides forwarding between each pair of hosts via MyTunnel protocol",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.p4tutorial.pipeconf",
    ],
    title = "MyTunnel Demo App",
    url = "http://onosproject.org",
)
