#!/bin/bash

host=${1:-localhost}

### Set up debug log messages for classes we care about
#
# -- NOTE: leave commented out for checked-in source
#          developer can uncomment locally

onos ${host} <<-EOF

#log:set DEBUG org.onosproject.ui.impl.topo.Topo2ViewMessageHandler
#log:set DEBUG org.onosproject.ui.impl.topo.Topo2Jsonifier
#log:set DEBUG org.onosproject.ui.impl.topo.Topo2TrafficMessageHandler
#log:set DEBUG org.onosproject.ui.impl.topo.Traffic2Monitor

#log:set DEBUG org.onosproject.ui.impl.UiWebSocket
#log:set DEBUG org.onosproject.ui.impl.UiTopoSession

#log:set DEBUG org.onosproject.ui.impl.topo.model.ModelCache

log:list

EOF
