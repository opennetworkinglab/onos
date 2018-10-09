COMPILE_DEPS = CORE_DEPS + KRYO + CLI + [
    "//core/store/serializers:onos-core-serializers",
    "//core/store/primitives:onos-core-primitives",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.fwd"],
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Traffic Engineering",
    description = "Provisions traffic between end-stations using hop-by-hop flow programming by " +
                  "intercepting packets for which there are currently no matching flow objectives on the " +
                  "data plane. The paths paved in this manner are short-lived, i.e. they expire a few " +
                  "seconds after the flow on whose behalf they were programmed stops.\n\n" +
                  "The application relies on the ONOS path service to compute the shortest paths. " +
                  "In the event of negative topology events (link loss, device disconnect, etc.), " +
                  "the application will proactively invalidate any paths that it had programmed to lead " +
                  "through the resources that are no longer available.",
    title = "Reactive Forwarding",
    url = "http://onosproject.org",
)
