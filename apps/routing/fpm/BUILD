BUNDLES = [
    "//apps/routing/common:onos-apps-routing-common",
    "//apps/routing-api:onos-apps-routing-api",
    "//apps/routing/fpm/api:onos-apps-routing-fpm-api",
    "//apps/routing/fpm/app:onos-apps-routing-fpm-app",
    "//apps/routing/fpm/web:onos-apps-routing-fpm-web",
]

onos_app(
    app_name = "org.onosproject.fpm",
    category = "Utility",
    description = "Receives/Transmits routes from external routing daemon over FPM protocol",
    included_bundles = BUNDLES,
    required_apps = ["org.onosproject.route-service"],
    title = "FIB Push Manager (FPM) Route Receiver",
    url = "http://onosproject.org",
)
