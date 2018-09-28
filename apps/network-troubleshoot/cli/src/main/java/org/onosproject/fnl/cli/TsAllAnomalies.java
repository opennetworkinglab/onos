/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.fnl.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.fnl.intf.NetworkDiagnosticService;

/**
 * Search for all types of network anomalies.
 */
@Service
@Command(scope = "onos",
        name = "ts-all-anomalies",
        description = "search all types of network anomalies once",
        detailedDescription = "Report different information " +
                "for specific type of anomalies.")
public class TsAllAnomalies extends AbstractShellCommand {

    @Override
    protected void doExecute() {
        NetworkDiagnosticService service = getService(NetworkDiagnosticService.class);

        service.findAnomalies().forEach(a -> print(a.toString()));
    }
}
