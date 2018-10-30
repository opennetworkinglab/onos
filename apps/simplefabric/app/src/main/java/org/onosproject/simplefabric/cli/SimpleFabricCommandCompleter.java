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
package org.onosproject.simplefabric.cli;

import com.google.common.collect.Lists;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractChoicesCompleter;

import java.util.List;
import java.util.Arrays;
import java.util.Collections;

/**
 * SimpleFabric command completer.
 */
@Service
public class SimpleFabricCommandCompleter extends AbstractChoicesCompleter {

    private static final List<String> COMMAND_LIST =
        Arrays.asList("show", "intents", "reactive-intents", "refresh", "flush");

    @Override
    public List<String> choices() {
        if (commandLine.getArguments() == null) {
            return Collections.emptyList();
        }
        List<String> argList = Lists.newArrayList(commandLine.getArguments());
        String argOne = null;
        if (argList.size() > 1) {
            argOne = argList.get(1);
        }
        if (COMMAND_LIST.contains(argOne)) {
            return Collections.emptyList();
        } else {
            return COMMAND_LIST;
        }
    }
}

