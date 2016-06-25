/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.flowanalyzer;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;

/**
 * Analyzes flows for cycles and black holes.
 */
@Command(scope = "onos", name = "flow-analysis",
         description = "Analyzes flows for cycles and black holes")
public class FlowAnalysisCommand extends AbstractShellCommand {

    @Override
    protected void execute() {
        FlowAnalyzer service = get(FlowAnalyzer.class);
        print(service.analyze());
    }
}
