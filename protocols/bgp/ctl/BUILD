COMPILE_DEPS = CORE_DEPS + [
    "@jersey_client//jar",
    "@httpclient_osgi//jar",
    "@httpcore_osgi//jar",
    "@javax_ws_rs_api//jar",
    "@io_netty_netty_common//jar",
    "@io_netty_netty//jar",
    "//protocols/bgp/api:onos-protocols-bgp-api",
    "//protocols/bgp/bgpio:onos-protocols-bgp-bgpio",
]

osgi_jar_with_tests(
    exclude_tests = [
        "org.onosproject.bgp.BgpPeerChannelHandlerTest",
        "org.onosproject.bgp.BgpPeerFrameDecoderTest",
    ],
    deps = COMPILE_DEPS,
)
