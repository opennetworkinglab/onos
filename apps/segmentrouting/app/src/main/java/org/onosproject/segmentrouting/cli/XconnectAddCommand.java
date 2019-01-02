/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.segmentrouting.cli;

import com.google.common.collect.Sets;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.segmentrouting.xconnect.api.XconnectEndpoint;
import org.onosproject.segmentrouting.xconnect.api.XconnectPortEndpoint;
import org.onosproject.segmentrouting.xconnect.api.XconnectService;

import java.util.Set;

/**
 * Creates Xconnect.
 */
@Command(scope = "onos", name = "sr-xconnect-add", description = "Create Xconnect")
public class XconnectAddCommand extends AbstractShellCommand {
    private static final String EP_DESC = "Can be a physical port number or a load balancer key. " +
            "Use integer to specify physical port number. " +
            "Use " + XconnectPortEndpoint.LB_KEYWORD + "key to specify load balancer key";

    @Argument(index = 0, name = "deviceId",
            description = "Device ID",
            required = true, multiValued = false)
    private String deviceIdStr;

    @Argument(index = 1, name = "vlanId",
            description = "VLAN ID",
            required = true, multiValued = false)
    private String vlanIdStr;

    @Argument(index = 2, name = "ep1",
            description = "First endpoint. " + EP_DESC,
            required = true, multiValued = false)
    private String ep1Str;

    @Argument(index = 3, name = "ep2",
            description = "Second endpoint. " + EP_DESC,
            required = true, multiValued = false)
    private String ep2Str;

    @Override
    protected void execute() {
        DeviceId deviceId = DeviceId.deviceId(deviceIdStr);
        VlanId vlanId = VlanId.vlanId(vlanIdStr);

        XconnectEndpoint ep1 = XconnectEndpoint.fromString(ep1Str);
        XconnectEndpoint ep2 = XconnectEndpoint.fromString(ep2Str);

        Set<XconnectEndpoint> endpoints = Sets.newHashSet(ep1, ep2);

        XconnectService xconnectService = get(XconnectService.class);
        xconnectService.addOrUpdateXconnect(deviceId, vlanId, endpoints);
    }


}
