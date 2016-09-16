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

package org.onosproject.vpls.cli;

import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.vpls.config.VplsConfigurationService;

import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * CLI to show VPLS details.
 */
@Command(scope = "onos", name = "vpls-show",
        description = "Shows the details of an existing VPLS")
public class VplsShowCommand extends AbstractShellCommand {

    private static final String NAME_FORMAT = "%10s: interface=%s";
    private static final String NETWORK_NOT_FOUND =
            "VPLS with name \'%s\' not found";
    private VplsConfigurationService vplsConfigService =
            get(VplsConfigurationService.class);

    @Argument(index = 0, name = "NETWORK_NAME", description = "Name of the VPLS",
            required = false, multiValued = false)
    private String vplsName = null;

    @Override
    protected void execute() {
        Set<String> vplsNames = vplsConfigService.getAllVpls();
        SetMultimap<String, Interface> vplsNetowrks = vplsConfigService.getVplsNetworks();
        Set<String> ifaceNames = Sets.newHashSet();


        if (!isNullOrEmpty(vplsName)) {

            if (vplsNames.contains(vplsName)) {
                vplsNetowrks.get(vplsName).stream()
                        .map(Interface::name)
                        .forEach(ifaceNames::add);
                print(NAME_FORMAT, vplsName, ifaceNames);
            } else {
                print(NETWORK_NOT_FOUND, vplsName);
            }
        } else {
            vplsNames.forEach(vplsName -> {
                ifaceNames.clear();
                vplsNetowrks.get(vplsName).stream()
                        .map(Interface::name)
                        .forEach(ifaceNames::add);
                print(NAME_FORMAT, vplsName, ifaceNames);
            });
        }
    }
}
