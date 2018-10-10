BUNDLES = [
    "//apps/dhcprelay/web:onos-apps-dhcprelay-web",
    "//apps/dhcprelay/app:onos-apps-dhcprelay-app",
]

onos_app(
    app_name = "org.onosproject.dhcprelay",
    category = "Utility",
    description = "DHCP Relay Agent Application.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.route-service",
        "org.onosproject.fpm",
    ],
    title = "DHCP Relay Agent",
    url = "http://onosproject.org",
)
