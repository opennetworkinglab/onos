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

package org.onosproject.virtualbng.cli;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.virtualbng.VbngConfigurationService;

/**
 * Command to show the list of vBNG IP address mapping entries.
 */
@Service
@Command(scope = "onos", name = "vbngs",
        description = "Lists all vBNG IP address mapping entries")
public class MappingListCommand extends AbstractShellCommand {

    private static final String FORMAT_HEADER =
            "   Private IP - Public IP";
    private static final String FORMAT_MAPPING =
            "   %s - %s";

    @Override
    protected void doExecute() {

        VbngConfigurationService service =
                AbstractShellCommand.get(VbngConfigurationService.class);

        // Print all mapping entries
        printMappingEntries(service.getIpAddressMappings());
    }

    /**
     * Prints all vBNG IP address mapping entries.
     *
     * @param map the map from private IP address to public address
     */
    private void printMappingEntries(Map<IpAddress, IpAddress> map) {
        print(FORMAT_HEADER);

        Iterator<Entry<IpAddress, IpAddress>> entries =
                map.entrySet().iterator();
        while (entries.hasNext()) {
            Entry<IpAddress, IpAddress> entry = entries.next();
            print(FORMAT_MAPPING, entry.getKey(), entry.getValue());
        }

        print("");
    }
}