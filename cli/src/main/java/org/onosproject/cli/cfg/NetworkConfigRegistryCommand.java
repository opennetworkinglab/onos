/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.cli.cfg;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistry;

/**
 * Displays network configuration registry contents.
 */
@Service
@Command(scope = "onos", name = "netcfg-registry",
        description = "Displays network configuration registry contents")
public class NetworkConfigRegistryCommand extends AbstractShellCommand {

    private static final String FMT = "subjectKey=%s, configKey=%s, subjectClass=%s, configClass=%s";
    private static final String SHORT_FMT = "%-12s %-12s %-40s %s";

    @Option(name = "-s", aliases = "--short", description = "Show short output only",
            required = false, multiValued = false)
    private boolean shortOnly = false;

    @Override
    protected void doExecute() {
        get(NetworkConfigRegistry.class).getConfigFactories().forEach(this::print);
    }

    private void print(ConfigFactory configFactory) {
        print(shortOnly ? SHORT_FMT : FMT,
              configFactory.subjectFactory().subjectClassKey(),
              configFactory.configKey(),
              configFactory.subjectFactory().subjectClass().getName(),
              configFactory.configClass().getName());
    }

}
