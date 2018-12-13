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


import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.service.PiPipeconfService;

import java.util.ArrayList;
import java.util.Collection;


/**
 * Query registered pipeconfs.
 */
@Service
@Command(scope = "onos", name = "pipeconfs",
        description = "List registered pipeconfs")
public class PipeconfCommand extends AbstractShellCommand {

    protected PiPipeconfService piPipeconfService;

    @Option(name = "-s", aliases = "--short",
            description = "Print more succinct output for each pipeconf",
            required = false, multiValued = false)
    private boolean shortOutput = false;

    @Override
    protected void doExecute() {
        piPipeconfService = get(PiPipeconfService.class);

        for (PiPipeconf piPipeconf : piPipeconfService.getPipeconfs()) {
            if (shortOutput) {
                print("id=%s", piPipeconf.id().toString());
            } else {
                print("id=%s, behaviors=%s, extensions=%s", piPipeconf.id().toString(),
                      getBehaviors(piPipeconf), getExtensions(piPipeconf));
            }
        }
    }

    /**
     * Get all behaviour of a pipeconf and converts a list of behaviour name to string.
     *
     * @param piPipeconf    query PiPipeconf
     *
     * @return string of behaviour name list
     */
    private String getBehaviors(PiPipeconf piPipeconf) {
        Collection<Class<? extends Behaviour>> behaviours = piPipeconf.behaviours();
        ArrayList<String> result = new ArrayList<>();

        for (Class<? extends Behaviour> behaviour:behaviours) {
            result.add(behaviour.getSimpleName());
        }

        return result.toString();
    }

    /**
     * Get all extension of a pipeconf and converts a list of extension
     * name to string.
     *
     * @param piPipeconf    query PiPipeconf
     *
     * @return string of extension name list
     */
    private String getExtensions(PiPipeconf piPipeconf) {
        ArrayList<String> result = new ArrayList<>();

        for (PiPipeconf.ExtensionType extensionType : PiPipeconf.ExtensionType.values()
             ) {
            if (piPipeconf.extension(extensionType).isPresent()) {
                result.add(extensionType.name());
            }
        }

        return result.toString();
    }
}
