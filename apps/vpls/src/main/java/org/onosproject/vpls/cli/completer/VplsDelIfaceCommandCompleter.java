/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.vpls.cli.completer;

import org.onosproject.cli.AbstractChoicesCompleter;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.vpls.config.VplsConfigService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.cli.AbstractShellCommand.get;

/**
 * Completer for vpls-del-iface command.
 */
public class VplsDelIfaceCommandCompleter extends AbstractChoicesCompleter {

    @Override
    protected List<String> choices() {
        VplsConfigService vplsConfigService =
                get(VplsConfigService.class);
        Set<Interface> ifaces = vplsConfigService.allIfaces();
        return ifaces.stream().map(Interface::name).collect(Collectors.toList());
    }
}
