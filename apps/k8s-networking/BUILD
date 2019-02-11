BUNDLES = [
    "//apps/k8s-networking/api:onos-apps-k8s-networking-api",
    "//apps/k8s-networking/app:onos-apps-k8s-networking-app",
    "@commons_net//jar",
    "@k8s_client//jar",
    "@k8s_model//jar",
]

onos_app(
    category = "Integration",
    description = "SONA Kubernetes Networking Application.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.k8s-node",
    ],
    title = "Kubernetes Networking Application",
    url = "https://wiki.onosproject.org/display/ONOS/SONA%3A+DC+Network+Virtualization",
)
