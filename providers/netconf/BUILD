BUNDLES = [
    "@sshd_core//jar",
    "@jaxb_api//jar",
    "//providers/netconf/device:onos-providers-netconf-device",
    "//providers/netconf/alarm:onos-providers-netconf-alarm",
    "//protocols/netconf/api:onos-protocols-netconf-api",
    "//protocols/netconf/ctl:onos-protocols-netconf-ctl",
]

EXCLUDED_BUNDLES = [
    "@ganymed_ssh2//jar",
]

onos_app(
    category = "Provider",
    description = "Provides means for ONOS to discover and trigger the initial handshake procedure " +
                  "with NETCONF from information given by network configuration." +
                  "Also provides capabilities for receiving asynchronous alarm notifications from devices.",
    excluded_bundles = EXCLUDED_BUNDLES,
    included_bundles = BUNDLES,
    required_apps = ["org.onosproject.faultmanagement"],
    title = "NETCONF Provider",
    url = "https://wiki.onosproject.org/display/ONOS/NETCONF",
)
