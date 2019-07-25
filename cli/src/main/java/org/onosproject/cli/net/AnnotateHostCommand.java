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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.completer.AnnotationKeysCompleter;
import org.onosproject.net.HostId;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.HostAnnotationConfig;
import org.onosproject.net.provider.ProviderId;

/**
 * Annotates host model.
 */
@Service
@Command(scope = "onos", name = "annotate-host",
        description = "Annotates host")
public class AnnotateHostCommand extends AbstractShellCommand {

    static final ProviderId PID = new ProviderId("cli", "org.onosproject.cli");

    @Argument(index = 0, name = "uri", description = "Host ID",
            required = true, multiValued = false)
    @Completion(HostIdCompleter.class)
    String uri = null;

    @Argument(index = 1, name = "key", description = "Annotation key",
            required = true, multiValued = false)
    @Completion(AnnotationKeysCompleter.class)
    String key = null;

    @Argument(index = 2, name = "value",
            description = "Annotation value (null to remove)",
            required = false, multiValued = false)
    String value = null;

    @Option(name = "--remove-config",
            description = "Remove annotation config")
    private boolean removeCfg = false;

    @Override
    protected void doExecute() {
        NetworkConfigService netcfgService = get(NetworkConfigService.class);
        HostId hostId = HostId.hostId(uri);

        if (key == null) {
            print("[ERROR] Annotation key not specified.");
            return;
        }
        HostAnnotationConfig cfg = netcfgService.getConfig(hostId, HostAnnotationConfig.class);
        if (cfg == null) {
            cfg = new HostAnnotationConfig(hostId);
        }
        if (removeCfg) {
            // remove config about entry
            cfg.annotation(key);
        } else {
            // add request config
            cfg.annotation(key, value);
        }
        netcfgService.applyConfig(hostId, HostAnnotationConfig.class, cfg.node());
    }
}
