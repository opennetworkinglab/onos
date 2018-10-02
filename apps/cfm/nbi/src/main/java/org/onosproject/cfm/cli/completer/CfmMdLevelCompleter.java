/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.cfm.cli.completer;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractChoicesCompleter;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain;

import java.util.ArrayList;
import java.util.List;

/**
 * CLI completer for MD Level creation.
 */
@Service
public class CfmMdLevelCompleter extends AbstractChoicesCompleter {
    @Override
    public List<String> choices() {
        List<String> choices = new ArrayList<>();

        for (MaintenanceDomain.MdLevel mdLevel:MaintenanceDomain.MdLevel.values()) {
            choices.add(String.valueOf(mdLevel.toString()));
        }

        return choices;
    }

}
