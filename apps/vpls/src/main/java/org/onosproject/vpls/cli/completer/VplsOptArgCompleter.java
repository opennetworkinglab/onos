/*
 * Copyright 2015-present Open Networking Foundation
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

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractChoicesCompleter;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.vpls.api.Vpls;
import org.onosproject.vpls.api.VplsData;
import org.onosproject.vpls.cli.VplsCommandEnum;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.cli.AbstractShellCommand.get;

/**
 * VPLS optional argument completer.
 */
@Service
public class VplsOptArgCompleter extends AbstractChoicesCompleter {
    protected Vpls vpls;
    protected InterfaceService interfaceService;

    @Override
    public List<String> choices() {
        if (vpls == null) {
            vpls = get(Vpls.class);
        }
        String[] argList = commandLine.getArguments();
        if (argList == null) {
            return Collections.emptyList();
        }
        String argOne = argList[1];
        VplsCommandEnum vplsCommandEnum = VplsCommandEnum.enumFromString(argOne);
        if (vplsCommandEnum != null) {
            switch (vplsCommandEnum) {
                case ADD_IFACE:
                    return availableIfaces();
                case SET_ENCAP:
                    return encap();
                case REMOVE_IFACE:
                    return vplsIfaces();
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
    private List<String> availableIfaces() {
        if (interfaceService == null) {
            interfaceService = get(InterfaceService.class);
        }

        Set<Interface> allIfaces = interfaceService.getInterfaces();
        Set<Interface> usedIfaces = vpls.getAllVpls().stream()
                .map(VplsData::interfaces)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        return allIfaces.stream()
                .filter(iface -> !usedIfaces.contains(iface))
                .map(Interface::name)
                .collect(Collectors.toList());
    }

    /**
     * Returns the list of supported encapsulation types.
     *
     * @return the list of supported encapsulation types
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
    private List<String> vplsIfaces() {
        String vplsName = commandLine.getArguments()[2];
        VplsData vplsData = vpls.getVpls(vplsName);
        return vplsData.interfaces().stream()
                .map(Interface::name)
                .collect(Collectors.toList());
    }
}
