COMPILE_DEPS = CORE_DEPS + [
    "@tinder_xmpp//jar",
    "@concurrent_hashmap//jar",
    "@gnu_idn//jar",
    "//protocols/xmpp/pubsub/api:onos-protocols-xmpp-pubsub-api",
    "//protocols/xmpp/core/api:onos-protocols-xmpp-core-api",
]

TEST_DEPS = TEST + [
    "//core/api:onos-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
