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
import org.onosproject.pim.impl.PIMInterface;
import org.onosproject.pim.impl.PIMInterfaces;
import org.onosproject.pim.impl.PIMInterfacesCodec;

import java.util.Collection;

@Command(scope = "onos", name = "pim-interfaces", description = "Displays the pim interfaces")
public class PIMShowCommand extends AbstractShellCommand {

    // prints either the json or cli version of the hash map connect point
    // neighbors from the PIMInterfaces class.
    @Override
    protected  void execute() {
        // grab connect point neighbors hash map to send in to json encoder.
        Collection<PIMInterface> pimIntfs = PIMInterfaces.getInstance().getInterfaces();
        if (outputJson()) {
            print("%s", json(pimIntfs));
        } else {
            print(PIMInterfaces.getInstance().printInterfaces());
        }
    }

    private JsonNode json(Collection<PIMInterface> pimIntfs) {
        return new PIMInterfacesCodec().encode(pimIntfs, this);
    }

}