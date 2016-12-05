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
package org.onosproject.vpls.cli;

import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.EncapsulationType;
import org.onosproject.vpls.config.VplsConfigService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * CLI to interact with the VPLS application.
 */
@Command(scope = "onos", name = "vpls",
        description = "Manages the VPLS application")
public class VplsCommand extends AbstractShellCommand {

    // Color codes and style
    private static final String BOLD = "\u001B[1m";
    private static final String COLOR_ERROR = "\u001B[31m";
    private static final String RESET = "\u001B[0m";

    // Messages and string formatter
    private static final String ENCAP_NOT_FOUND =
            COLOR_ERROR + "Encapsulation type " + BOLD + "%s" + RESET +
                    COLOR_ERROR + " not found" + RESET;

    private static final String IFACE_NOT_FOUND =
            COLOR_ERROR + "Interface " + BOLD + "%s" + RESET + COLOR_ERROR +
                    " not found" + RESET;

    private static final String IFACE_ALREADY_ASSOCIATED =
            COLOR_ERROR + "Interface " + BOLD + "%s" + RESET + COLOR_ERROR +
                    " already associated to VPLS " + BOLD + "%s" + RESET +
                    COLOR_ERROR + "" + RESET;

    private static final String SEPARATOR = "----------------";

    private static final String VPLS_ALREADY_EXISTS =
            COLOR_ERROR + "VPLS " + BOLD + "%s" + RESET + COLOR_ERROR +
                    " already exists" + RESET;

    private static final String VPLS_COMMAND_NOT_FOUND =
            COLOR_ERROR + "VPLS command " + BOLD + "%s" + RESET + COLOR_ERROR +
                    " not found" + RESET;

    private static final String VPLS_DISPLAY = "VPLS name: " + BOLD +
            "%s" + RESET + "\nAssociated interfaces: %s\nEncapsulation: %s";

    private static final String VPLS_LIST_TITLE =
            BOLD + "Configured VPLSs" + RESET;

    private static final String VPLS_NOT_FOUND =
            COLOR_ERROR + "VPLS " + BOLD + "%s" + RESET + COLOR_ERROR +
                    " not found" + RESET;

    private static final String IFACE_NOT_ASSOCIATED =
            COLOR_ERROR + "Interface " + BOLD + "%s" + RESET + COLOR_ERROR +
                    " cannot be removed from VPLS " + BOLD + "%s" + RESET +
                    COLOR_ERROR + ". The interface is associated to another" +
                    "VPLS (" + BOLD + "%s" + RESET + COLOR_ERROR + ")" + RESET;

    private static VplsConfigService vplsConfigService =
            get(VplsConfigService.class);
    private static InterfaceService interfaceService =
            get(InterfaceService.class);

    @Argument(index = 0, name = "command", description = "Command name (add|" +
            "clean|create|delete|list|removeIface|set-encap|show)",
            required = true, multiValued = false)
    String command = null;

    @Argument(index = 1, name = "vplsName", description = "The name of the VPLS",
            required = false, multiValued = false)
    String vplsName = null;

    @Argument(index = 2, name = "optArg", description = "The interface name for" +
            "all commands; the encapsulation type for set-encap",
            required = false, multiValued = false)
    String optArg = null;

    @Override
    protected void execute() {
        VplsCommandEnum enumCommand = VplsCommandEnum.enumFromString(command);
        if (enumCommand != null) {
            switch (enumCommand) {
                case ADD_IFACE:
                    addIface(vplsName, optArg);
                    break;
                case CLEAN:
                    clean();
                    break;
                case CREATE:
                    create(vplsName);
                    break;
                case DELETE:
                    delete(vplsName);
                    break;
                case LIST:
                    list();
                    break;
                case REMOVE_IFACE:
                    removeIface(vplsName, optArg);
                    break;
                case SET_ENCAP:
                    setEncap(vplsName, optArg);
                    break;
                case SHOW:
                    show(vplsName);
                    break;
                default:
                    print(VPLS_COMMAND_NOT_FOUND, command);
            }
        }
    }

    /**
     * Adds an inteterface to a VPLS.
     *
     * @param vplsName the name of the VLPS
     * @param ifaceName the name of the interface to add
     */
    private void addIface(String vplsName, String ifaceName) {
        // Check if the VPLS exists
        if (!vplsExists(vplsName)) {
            print(VPLS_NOT_FOUND, vplsName);
            return;
        }
        // Check if the interface exists
        if (!ifaceExists(ifaceName)) {
            print(IFACE_NOT_FOUND, ifaceName);
            return;
        }
        // Check if the interface is already associated to a VPLS
        if (isIfaceAssociated(ifaceName)) {
            print(IFACE_ALREADY_ASSOCIATED,
                  ifaceName, vplsNameFromIfaceName(ifaceName));
            return;
        }

        vplsConfigService.addIface(vplsName, ifaceName);
    }

    /**
     * Cleans the VPLS configuration.
     */
    private void clean() {
        vplsConfigService.cleanVplsConfig();
    }

    /**
     * Creates a new VPLS.
     *
     * @param vplsName the name of the VLPS
     */
    private void create(String vplsName) {
        if (vplsExists(vplsName)) {
            print(VPLS_ALREADY_EXISTS, vplsName);
            return;
        }
        vplsConfigService.addVpls(vplsName, Sets.newHashSet(), null);
    }

    /**
     * Deletes a VPLS.
     *
     * @param vplsName the name of the VLPS
     */
    private void delete(String vplsName) {
        if (!vplsExists(vplsName)) {
            print(VPLS_NOT_FOUND, vplsName);
            return;
        }
        vplsConfigService.removeVpls(vplsName);
    }

    /**
     * Lists the configured VPLSs.
     */
    private void list() {
        List<String> vplsNames = Lists.newArrayList(vplsConfigService.vplsNames());
        Collections.sort(vplsNames);

        print(VPLS_LIST_TITLE);
        print(SEPARATOR);
        vplsNames.forEach(vpls -> {
            print(vpls);
        });
    }

    /**
     * Removes an interface from a VPLS.
     *
     * @param vplsName the name of the VLPS
     * @param ifaceName the name of the interface to remove
     */
    private void removeIface(String vplsName, String ifaceName) {
        if (!vplsExists(vplsName)) {
            print(VPLS_NOT_FOUND, vplsName);
            return;
        }
        if (!ifaceExists(ifaceName)) {
            print(IFACE_NOT_FOUND, ifaceName);
            return;
        }
        String vplsNameFromIfaceName = vplsNameFromIfaceName(ifaceName);
        if (!vplsNameFromIfaceName.equals(vplsName)) {
            print(IFACE_NOT_ASSOCIATED, ifaceName, vplsName, vplsNameFromIfaceName);
            return;
        }
        vplsConfigService.removeIface(ifaceName);
    }

    /**
     * Sets the encapsulation type for a VPLS.
     *
     * @param vplsName the name of the VPLS
     * @param encap the encapsulation type
     */
    private void setEncap(String vplsName, String encap) {
        if (!vplsExists(vplsName)) {
            print(VPLS_NOT_FOUND, vplsName);
            return;
        }
        EncapsulationType encapType = EncapsulationType.enumFromString(encap);
        if (encapType.equals(EncapsulationType.NONE) &&
            !encapType.toString().equals(encap)) {
            print(ENCAP_NOT_FOUND, encap);
            return;
        }
        vplsConfigService.setEncap(vplsName, encap);
    }

    /**
     * Shows the details of one or more VPLSs.
     *
     * @param vplsName the name of the VPLS
     */
    private void show(String vplsName) {
        List<String> vplsNames = Lists.newArrayList(vplsConfigService.vplsNames());
        Collections.sort(vplsNames);

        Map<String, EncapsulationType> encapByVplsName =
                vplsConfigService.encapByVplsName();

        print(VPLS_LIST_TITLE);
        print(SEPARATOR);
        if (!isNullOrEmpty(vplsName)) {
            // A VPLS name is provided. Check first if the VPLS exists
            if (vplsExists(vplsName)) {
                print(VPLS_DISPLAY,
                      vplsName,
                      ifacesFromVplsName(vplsName).toString(),
                      encapByVplsName.get(vplsName).toString());
            } else {
                print(VPLS_NOT_FOUND, vplsName);
            }
        } else {
            // No VPLS names are provided. Display all VPLSs configured
            vplsNames.forEach(v -> {
                print(VPLS_DISPLAY,
                      v,
                      ifacesFromVplsName(v).toString(),
                      encapByVplsName.get(v).toString());
                print(SEPARATOR);
            });
        }
    }

    /**
     * States if a VPLS exists or not.
     *
     * @param vplsName the name of the VPLS
     * @return true if the VPLS exists; false otherwise
     */
    private static boolean vplsExists(String vplsName) {
        return vplsConfigService.vplsNames().contains(vplsName);
    }

    /**
     * States if an interface is defined or not in the system.
     *
     * @param ifaceName the name of the interface
     * @return true if the interface is defined; false otherwise
     */
    private static boolean ifaceExists(String ifaceName) {
        return vplsConfigService.allIfaces()
                .stream()
                .anyMatch(iface -> iface.name().equals(ifaceName));
    }

    /**
     * States if an interface is already associated to a VPLS.
     *
     * @param ifaceName the name of the interface
     * @return true if the interface is already associated to a VPLS; false
     * otherwise
     */
    private static boolean isIfaceAssociated(String ifaceName) {
        return vplsConfigService.ifaces()
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
    private static String vplsNameFromIfaceName(String ifaceName) {
        String vplsName = null;

        Optional<String> optVplsName = vplsConfigService.ifacesByVplsName()
                .entries()
                .stream()
                .filter((entry -> entry.getValue().name().equals(ifaceName)))
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
    private static Set<String> ifacesFromVplsName(String vplsName) {
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
