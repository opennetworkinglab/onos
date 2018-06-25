BUNDLES = [
    '//apps/t3/web:onos-apps-t3-web',
    '//apps/t3/app:onos-apps-t3-app',
]

onos_app (
   title = 'Trellis Troubleshooting Toolkit',
   category = 'Utilities',
   url = 'https://wiki.opencord.org/pages/viewpage.action?pageId=4456974',
   description = 'Provides static analysis of flows and groups ' +
   'to determine the possible paths a packet may take.',
   required_apps = [
           'org.onosproject.segmentrouting',
           'org.onosproject.route-service',
           'org.onosproject.mcast',
   ],
   included_bundles = BUNDLES,
   app_name = 'org.onosproject.t3'
)
