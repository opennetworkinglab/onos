BUNDLES = [
    "//apps/openstacknetworking/api:onos-apps-openstacknetworking-api",
    "//apps/openstacknetworking/app:onos-apps-openstacknetworking-app",
    "@httpclient_osgi//jar",
    "@httpcore_osgi//jar",
    "@commons_net//jar",
    "@commons_codec//jar",
    "@sshd_core//jar",
]

onos_app(
    category = "Integration",
    description = "SONA Openstack Networking Application.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.openstacknode",
    ],
    title = "OpenStack Networking Application",
    url = "https://wiki.onosproject.org/display/ONOS/SONA%3A+DC+Network+Virtualization",
)
