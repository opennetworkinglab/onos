BUNDLES = [
    '@jersey_client//jar',
    '@jersey_security//jar',
    '@commons_io//jar',
    '@httpclient_osgi//jar',
    '@httpcore_osgi//jar',
    '//providers/rest/device:onos-providers-rest-device',
    '//protocols/rest/api:onos-protocols-rest-api',
    '//protocols/rest/ctl:onos-protocols-rest-ctl',
]

onos_app (
    app_name = 'org.onosproject.restsb',
    title = 'REST Provider',
    category = 'Provider',
    url = 'https://wiki.onosproject.org/display/ONOS/REST',
    included_bundles = BUNDLES,
    description = 'Provides means for ONOS to discover the existence of a REST API enabled device ' +
        'or proxy for multiple devices from information given by network configuration.',
)

