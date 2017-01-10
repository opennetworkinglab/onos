/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.cli.net.completer;

import static org.onlab.osgi.DefaultServiceDirectory.getService;
import static org.onosproject.net.ConnectPoint.deviceConnectPoint;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.karaf.shell.console.completer.ArgumentCompleter.ArgumentList;
import org.onosproject.cli.AbstractChoicesCompleter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkService;

/**
 * Completer, which proposes remote end of existing Link in the system.
 * <p>
 * This completer will look for (device id)/(port number) in the
 * existing argument and propose list of remote ports.
 */
public class PeerConnectPointCompleter extends AbstractChoicesCompleter {

    @Override
    protected List<String> choices() {
        ArgumentList args = getArgumentList();

        DeviceService deviceService = getService(DeviceService.class);
        LinkService linkService = getService(LinkService.class);

        Optional<ConnectPoint> port = Arrays.asList(args.getArguments()).stream()
            .filter(s -> s.contains(":") && s.contains("/"))
            .map(s -> {
                try {
                    return deviceConnectPoint(s);
                } catch (IllegalArgumentException e) {
                    // silently ill-formed String
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .filter(cp -> deviceService.getPort(cp) != null)
            .findFirst();

        if (!port.isPresent()) {
            // no candidate
            return Collections.emptyList();
        }
        final ConnectPoint cp = port.get();

        return linkService.getLinks(cp).stream()
                .flatMap(l -> Stream.of(l.src(), l.dst()))
                .filter(peer -> !cp.equals(peer))
                .distinct()
                .map(ConnectPoint::toString)
                .collect(Collectors.toList());
    }

}
