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
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.vpls.config.VplsConfigService;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.onlab.osgi.DefaultServiceDirectory.getService;

/**
 * Util class for VPLS Commands.
 */
public final class VplsCommandUtils {

    protected static final String VPLS_NAME = "VPLS name %s: ";
    protected static final String VPLS_DISPLAY = "VPLS name: %s, associated " +
            "interfaces: %s, encapsulation: %s";
    protected static final String VPLS_NOT_FOUND = "VPLS %s not found.";
    protected static final String IFACE_NOT_FOUND = "Interface %s not found.";
    protected static final String VPLS_ALREADY_EXISTS = "VPLS %s already exists.";
    protected static final String IFACE_ALREADY_ASSOCIATED =
            "Interface %s already associated to VPLS %s.";
    protected static final String IFACE_NOT_ASSOCIATED =
            "Interface %s is associated to VPLS %s.";

    private static VplsConfigService vplsConfigService =
            getService(VplsConfigService.class);
    private static InterfaceService interfaceService =
            getService(InterfaceService.class);

    private VplsCommandUtils() {}

    /**
     * States if a VPLS exists or not.
     *
     * @param vplsName the name of the VPLS
     * @return true if the VPLS exists; false otherwise
     */
    protected static boolean vplsExists(String vplsName) {
        return vplsConfigService.vplsNames().contains(vplsName);
    }

    /**
     * States if an interface is defined or not in the system.
     *
     * @param ifaceName the name of the interface
     * @return true if the interface is defined; false otherwise
     */
    protected static boolean ifaceExists(String ifaceName) {
        return interfaceService.getInterfaces()
                .stream()
                .anyMatch(iface -> iface.name().equals(ifaceName));
    }

    /**
     * States if an interface is already associated or not to a VPLS.
     *
     * @param ifaceName the name of the interface
     * @return true if the interface is already associated to a VPLS; false
     * otherwise
     */
    protected static boolean ifaceAlreadyAssociated(String ifaceName) {
        return vplsConfigService.allIfaces()
                .stream()
                .anyMatch(iface -> iface.name().equals(ifaceName));
    }

    /**
     * Returns the name of a VPLS, given the name of an interface associated to
     * it.
     *
     * @param ifaceName the name of the interface
     * @return the name of the VPLS that has the interface configured; null if
     * the interface does not exist or is not associated to any VPLS
     */
    protected static String vplsNameFromIfaceName(String ifaceName) {
        String vplsName = null;

        Optional<String> optVplsName = vplsConfigService.ifacesByVplsName()
                .entries()
                .stream()
                .filter(iface -> iface.getValue().name().equals(ifaceName))
                .map(Map.Entry::getKey)
                .findFirst();

        if (optVplsName.isPresent()) {
            vplsName = optVplsName.get();
        }

        return vplsName;
    }

    /**
     * Returns a list of interfaces associated to a VPLS, given a VPLS name.
     *
     * @param vplsName the name of the VPLS
     * @return the set of interfaces associated to the given VPLS; null if the
     * VPLS is not found
     */
    protected static Set<String> ifacesFromVplsName(String vplsName) {
        if (!vplsExists(vplsName)) {
            return null;
        }

        SetMultimap<String, Interface> ifacesByVplsName =
                vplsConfigService.ifacesByVplsName();
        Set<String> ifaceNames = Sets.newHashSet();

        ifacesByVplsName.get(vplsName).forEach(iface -> ifaceNames.add(iface.name()));

        return ifaceNames;
    }
}
