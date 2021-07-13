/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.pipelines.fabric.impl.behaviour.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.pipelines.fabric.impl.behaviour.upf.FabricUpfStore;
import org.onosproject.pipelines.fabric.impl.behaviour.upf.UpfRuleIdentifier;

import java.util.Map;

/**
 * Read internal UPF store of fabric.
 */
@Service
@Command(scope = "fabric", name = "upf-read-internal-store",
        description = "Print internal UPF stores")
public class ReadInternalUpfStoreCommand extends AbstractShellCommand {
    @Option(name = "-v", aliases = "--verbose",
            description = "Print more detail of each entry",
            required = false, multiValued = false)
    private boolean verbose = false;

    @Override
    protected void doExecute() {
        FabricUpfStore upfStore = get(FabricUpfStore.class);

        if (upfStore == null) {
            print("Error: FabricUpfStore is null");
            return;
        }

        Map<Integer, UpfRuleIdentifier> reverseFarIdMap = upfStore.getReverseFarIdMap();
        print("reverseFarIdMap size: " + reverseFarIdMap.size());
        if (verbose) {
            reverseFarIdMap.entrySet().forEach(entry -> print(entry.toString()));
        }
    }
}