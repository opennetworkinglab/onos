/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.newoptical.cli;

import java.util.List;
import java.util.stream.Collectors;

import org.onlab.util.Identifier;
import org.onosproject.cli.AbstractChoicesCompleter;
import org.onosproject.newoptical.OpticalConnectivity;
import org.onosproject.newoptical.api.OpticalPathService;

import static org.onosproject.cli.AbstractShellCommand.get;

/**
 * Completer for OpticalConnectivityId.
 */
public class OpticalConnectivityIdCompleter extends AbstractChoicesCompleter {

    @Override
    protected List<String> choices() {
        OpticalPathService opticalPathService = get(OpticalPathService.class);

        return opticalPathService.listConnectivity().stream()
                .map(OpticalConnectivity::id)
                .map(Identifier::id)
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

}
