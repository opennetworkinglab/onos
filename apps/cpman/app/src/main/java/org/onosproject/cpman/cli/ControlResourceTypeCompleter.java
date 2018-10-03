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
package org.onosproject.cpman.cli;

import com.google.common.collect.ImmutableList;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractChoicesCompleter;

import java.util.List;
import java.util.stream.Collectors;

import static org.onosproject.cpman.ControlResource.Type;

/**
 * Control resource type completer.
 */
@Service
public class ControlResourceTypeCompleter extends AbstractChoicesCompleter {

    private static final List<Type> RESOURCE_TYPES =
            ImmutableList.of(Type.CPU, Type.MEMORY, Type.DISK, Type.NETWORK,
                    Type.CONTROL_MESSAGE);

    @Override
    protected List<String> choices() {
        return RESOURCE_TYPES.stream().map(type ->
                type.toString().toLowerCase()).collect(Collectors.toList());
    }
}
