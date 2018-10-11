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

package org.onosproject.cli.net;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.IpAddress;
import org.onlab.util.Tools;
import org.onosproject.cli.AbstractChoicesCompleter;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.mcast.McastRoute;
import org.onosproject.net.mcast.MulticastRouteService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mcast group Completer.
 */
@Service
public class McastGroupCompleter extends AbstractChoicesCompleter {

    @Override
    protected List<String> choices() {
        MulticastRouteService service = AbstractShellCommand.get(MulticastRouteService.class);

        return Tools.stream(service.getRoutes())
            .map(McastRoute::group)
            .map(IpAddress::toString)
            .collect(Collectors.toList());
    }

}
