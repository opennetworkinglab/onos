/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.cli.net;

import static org.onosproject.net.LinkKey.linkKey;

import java.util.Optional;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.completer.PeerConnectPointCompleter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicLinkConfig;
import org.onosproject.net.device.DeviceService;

/**
 * Add Link configuration.
 */
@Service
@Command(scope = "onos", name = "config-link",
         description = "Configure link.")
public class ConfigureLinkCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "src", description = "src port",
            required = true, multiValued = false)
    @Completion(ConnectPointCompleter.class)
    String src = null;

    @Argument(index = 1, name = "dst", description = "dst port",
            required = true, multiValued = false)
    @Completion(PeerConnectPointCompleter.class)
    String dst = null;

    @Option(name = "--type",
            description = "specify link type",
            valueToShowInHelp = "DIRECT")
    String type = Link.Type.DIRECT.name();


    @Option(name = "--uni-directional",
            description = "specify that link is uni-directional")
    boolean isUniDi = false;

    // TODO add metric, latency, durable

    @Option(name = "--bandwidth",
            description = "bandwidth in Mbps (integer)")
    String bandwidth = null;

    @Option(name = "--disallow",
            description = "disallow link")
    boolean disallow = false;

    @Option(name = "--remove-config",
            description = "remove link configuration")
    boolean remove = false;


    @Override
    protected void doExecute() {
        DeviceService deviceService = get(DeviceService.class);
        NetworkConfigService netCfgService = get(NetworkConfigService.class);

        ConnectPoint srcCp = ConnectPoint.deviceConnectPoint(src);
        if (deviceService.getPort(srcCp) == null) {
            print("[ERROR] %s does not exist", srcCp);
            return;
        }

        ConnectPoint dstCp = ConnectPoint.deviceConnectPoint(dst);
        if (deviceService.getPort(dstCp) == null) {
            print("[ERROR] %s does not exist", dstCp);
            return;
        }

        LinkKey link = linkKey(srcCp, dstCp);
        if (remove) {
            netCfgService.removeConfig(link, BasicLinkConfig.class);
            return;
        }

        Long bw = Optional.ofNullable(bandwidth)
                        .map(Long::valueOf)
                        .orElse(null);

        Link.Type linkType = Link.Type.valueOf(type);

        BasicLinkConfig cfg = netCfgService.addConfig(link, BasicLinkConfig.class);
        cfg.isAllowed(!disallow);
        cfg.isBidirectional(!isUniDi);
        cfg.type(linkType);
        if (bw != null) {
            cfg.bandwidth(bw);
        }

        cfg.apply();
    }

}
