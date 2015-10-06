/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.pim.cli;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.pim.impl.PIMNeighbors;
import org.onosproject.pim.impl.PIMNeighborsCodec;

import java.util.HashMap;

@Command(scope = "onos", name = "pim-neighbors", description = "Displays the pim neighbors")
public class PIMShowCommand extends AbstractShellCommand {

    // prints either the json or cli version of the hash map connect point
    // neighbors from the PIMNeighbors class.
    @Override
    protected  void execute() {
        // grab connect point neighbors hash map to send in to json encoder.
        HashMap<ConnectPoint, PIMNeighbors> pimNbrs = PIMNeighbors.getConnectPointNeighbors();
        if (outputJson()) {
            print("%s", json(pimNbrs));
        } else {
            print(PIMNeighbors.printPimNeighbors());
        }
    }

    private JsonNode json(HashMap<ConnectPoint, PIMNeighbors> pimNbrs) {
        return new PIMNeighborsCodec().encode(pimNbrs, this);
    }

}