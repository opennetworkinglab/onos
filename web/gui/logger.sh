#!/bin/bash

host=${1:-localhost}

### Set up debug log messages for classes we care about
onos ${host} <<-EOF

log:set DEBUG org.onosproject.ui.impl.topo.Topo2ViewMessageHandler
log:set DEBUG org.onosproject.ui.impl.topo.Topo2Jsonifier
log:set DEBUG org.onosproject.ui.impl.topo.Topo2TrafficMessageHandler
log:set DEBUG org.onosproject.ui.impl.topo.Traffic2Monitor

#log:set DEBUG org.onosproject.ui.impl.UiWebSocket
#log:set DEBUG org.onosproject.ui.impl.UiTopoSession

log:list

EOF
