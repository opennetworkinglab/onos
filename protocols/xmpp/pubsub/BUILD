BUNDLES = [
    "@tinder_xmpp//jar",
    "@concurrent_hashmap//jar",
    "@gnu_idn//jar",
    "//protocols/xmpp/core/api:onos-protocols-xmpp-core-api",
    "//protocols/xmpp/pubsub/api:onos-protocols-xmpp-pubsub-api",
    "//protocols/xmpp/pubsub/ctl:onos-protocols-xmpp-pubsub-ctl",
]

onos_app(
    app_name = "org.onosproject.protocols.xmpp.pubsub",
    category = "Protocol",
    description = "XMPP Publish/Subscribe protocol extension subsystem",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.protocols.xmpp",
    ],
    title = "XMPP Publish/Subscribe protocol extension subsystem",
    url = "http://onosproject.org",
)
