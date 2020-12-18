BUNDLES = [
    "//apps/kubevirt-networking/api:onos-apps-kubevirt-networking-api",
    "//apps/kubevirt-networking/app:onos-apps-kubevirt-networking-app",
    "@commons_net//jar",
    "@jersey_client//jar",
    "@json//jar",
    "@k8s_client//jar",
    "@k8s_model//jar",
    "@okhttp//jar",
    "@okio//jar",
    "@logging_interceptor//jar",
    "@jackson_dataformat_yaml//jar",
    "@snakeyaml//jar",
    "@zjsonpatch//jar",
    "@validation_api//jar",
]

onos_app(
    category = "Integration",
    description = "SONA KubeVirt Networking Application.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.kubevirt-node",
    ],
    title = "KubeVirt Networking Application",
    url = "https://wiki.onosproject.org/display/ONOS/SONA%3A+DC+Network+Virtualization",
)
