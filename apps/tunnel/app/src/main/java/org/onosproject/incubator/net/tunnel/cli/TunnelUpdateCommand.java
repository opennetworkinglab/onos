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
package org.onosproject.incubator.net.tunnel.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.tunnel.DefaultTunnelDescription;
import org.onosproject.incubator.net.tunnel.TunnelDescription;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelProvider;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.SparseAnnotations;

/**
 * Supports for updating a tunnel by tunnel identity.
 * It's used by producers.
 */
@Service
@Command(scope = "onos", name = "tunnel-update",
description = "Supports for updating a tunnel by tunnel identity."
        + " It's used by producers.")
public class TunnelUpdateCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "tunnelId", description = "the tunnel identity.",
            required = true, multiValued = false)
    String tunnelId = null;

    @Option(name = "-b", aliases = "--bandwidth",
            description = "The bandwidth attribute of tunnel", required = false, multiValued = false)
    String bandwidth = null;

    @Override
    protected void doExecute() {
        TunnelProvider service = get(TunnelProvider.class);
        TunnelId id = TunnelId.valueOf(tunnelId);
        SparseAnnotations annotations = DefaultAnnotations
                .builder()
                .set("bandwidth", bandwidth)
                .build();
        TunnelDescription tunnel = new DefaultTunnelDescription(id, null,
                                                                null,
                                                                null, null,
                                                                null,
                                                                null, null, annotations);
        service.tunnelUpdated(tunnel);
    }

}
