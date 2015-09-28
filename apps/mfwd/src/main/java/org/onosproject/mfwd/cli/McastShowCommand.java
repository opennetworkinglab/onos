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
package org.onosproject.mfwd.cli;

import org.apache.karaf.shell.commands.Command;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.mfwd.impl.McastRouteTable;
import org.onosproject.mfwd.impl.MRibCodec;

import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Displays the source, multicast group flows entries.
 */
@Command(scope = "onos", name = "mcast-show", description = "Displays the source, multicast group flows")
public class McastShowCommand extends AbstractShellCommand {

    private final Logger log = getLogger(getClass());
    private static final String MCAST_GROUP = "mcastgroup";

    @Override
    protected void execute() {
        McastRouteTable mrt = McastRouteTable.getInstance();
        if (outputJson()) {
            print("%s", json(mrt));
        } else {
            printMrib4(mrt);
        }
    }

    public JsonNode json(McastRouteTable mrt) {
        ObjectNode pushContent = new MRibCodec().encode(mrt , this);
        return pushContent;
    }

    /**
     * Displays multicast route table entries.
     *
     * @param mrt Mutlicast Route Table
     */
    protected void printMrib4(McastRouteTable mrt) {
        print(mrt.printMcastRouteTable());
    }
}
