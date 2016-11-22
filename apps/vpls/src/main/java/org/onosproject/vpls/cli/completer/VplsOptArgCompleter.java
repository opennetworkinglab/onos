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
package org.onosproject.vpls.cli.completer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.karaf.shell.console.completer.ArgumentCompleter;
import org.onosproject.cli.AbstractChoicesCompleter;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.net.EncapsulationType;
import org.onosproject.vpls.cli.VplsCommandEnum;
import org.onosproject.vpls.config.VplsConfigService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.cli.AbstractShellCommand.get;

/**
 * VPLS optional argument completer.
 */
public class VplsOptArgCompleter extends AbstractChoicesCompleter {

    @Override
    public List<String> choices() {
        VplsConfigService vplsConfigService = get(VplsConfigService.class);
        List<String> argumentList =
                Lists.newArrayList(getArgumentList().getArguments());
        String argOne = argumentList.get(1);
        VplsCommandEnum vplsCommandEnum = VplsCommandEnum.enumFromString(argOne);
        if (vplsCommandEnum != null) {
            switch (vplsCommandEnum) {
                case ADD_IFACE:
                    return availableIfaces(vplsConfigService);
                case SET_ENCAP:
                    return encap();
                case REMOVE_IFACE:
                    return vplsIfaces(vplsConfigService);
                default:
                    return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }

    /**
     * Returns the list of interfaces not yet assigned to any VPLS.
     *
     * @return the list of interfaces not yet assigned to any VPLS
     */
    private List<String> availableIfaces(VplsConfigService vplsConfigService) {
        List<String> ifacesAvailable = Lists.newArrayList();
        Set<Interface> allIfaces = Sets.newHashSet(vplsConfigService.allIfaces());
        Set<Interface> usedIfaces = Sets.newHashSet(vplsConfigService.ifaces());

        allIfaces.removeAll(usedIfaces);
        allIfaces.forEach(iface -> ifacesAvailable.add(iface.name()));

        return ifacesAvailable;
    }

    /**
     * Returns the list of supported encapsulation types.
     *
     * @return the list of supported encapsualtion types
     */
    private List<String> encap() {
        return Arrays.stream(EncapsulationType.values())
                                              .map(Enum::toString)
                                              .collect(Collectors.toList());
    }

    /**
     * Returns the list of interfaces associated to a VPLS.
     *
     * @return the list of interfaces associated to a VPLS
     */
    private List<String> vplsIfaces(VplsConfigService vplsConfigService) {
        ArgumentCompleter.ArgumentList list = getArgumentList();
        String vplsName = list.getArguments()[2];

        List<String> vplsIfaces = Lists.newArrayList();

        Set<Interface> connectPoints = vplsConfigService.ifaces(vplsName);
        connectPoints.forEach(iface -> vplsIfaces.add(iface.name()));

        return vplsIfaces;
    }
}
