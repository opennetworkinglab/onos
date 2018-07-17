BUNDLES = [
    "//apps/t3/web:onos-apps-t3-web",
    "//apps/t3/app:onos-apps-t3-app",
]

onos_app(
    app_name = "org.onosproject.t3",
    category = "Utilities",
    description = "Provides static analysis of flows and groups " +
                  "to determine the possible paths a packet may take.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.segmentrouting",
        "org.onosproject.route-service",
        "org.onosproject.mcast",
    ],
    title = "Trellis Troubleshooting Toolkit",
    url = "https://wiki.opencord.org/pages/viewpage.action?pageId=4456974",
)
