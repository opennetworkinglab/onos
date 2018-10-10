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
package org.onosproject.vpls.cli;

import com.google.common.collect.ImmutableSet;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.EncapsulationType;
import org.onosproject.vpls.api.VplsData;
import org.onosproject.vpls.api.Vpls;
import org.onosproject.vpls.api.VplsData.VplsState;
import org.onosproject.vpls.cli.completer.VplsCommandCompleter;
import org.onosproject.vpls.cli.completer.VplsNameCompleter;
import org.onosproject.vpls.cli.completer.VplsOptArgCompleter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onosproject.vpls.api.VplsData.VplsState.*;


/**
 * CLI to interact with the VPLS application.
 */
@Service
@Command(scope = "onos", name = "vpls",
        description = "Manages the VPLS application")
public class VplsCommand extends AbstractShellCommand {
    private static final Set<VplsState> CHANGING_STATE =
            ImmutableSet.of(ADDING, REMOVING, UPDATING);

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

    private static final String INSERT_VPLS_NAME =
            COLOR_ERROR + "Missing the " + BOLD + "VPLS name." + RESET +
                    COLOR_ERROR + " Specifying a VPLS name is mandatory." +
                    RESET;

    private static final String INSERT_ENCAP_TYPE =
            COLOR_ERROR + "Missing the " + BOLD + "encapsulation type." +
                    RESET + COLOR_ERROR + " Encapsulation type is mandatory." +
                    RESET;

    private static final String INSERT_INTERFACE =
            COLOR_ERROR + "Missing the " + BOLD + "interface name." +
                    RESET + COLOR_ERROR + " Specifying an interface name is" +
                    " mandatory." + RESET;

    private static final String SEPARATOR = "----------------";

    private static final String VPLS_ALREADY_EXISTS =
            COLOR_ERROR + "VPLS " + BOLD + "%s" + RESET + COLOR_ERROR +
                    " already exists" + RESET;

    private static final String VPLS_COMMAND_NOT_FOUND =
            COLOR_ERROR + "VPLS command " + BOLD + "%s" + RESET + COLOR_ERROR +
                    " not found" + RESET;

    private static final String VPLS_DISPLAY = "VPLS name: " + BOLD +
            "%s" + RESET + "\nAssociated interfaces: %s\nEncapsulation: %s\n" +
            "State: %s";

    private static final String VPLS_NOT_FOUND =
            COLOR_ERROR + "VPLS " + BOLD + "%s" + RESET + COLOR_ERROR +
                    " not found" + RESET;

    private static final String IFACE_NOT_ASSOCIATED =
            COLOR_ERROR + "Interface " + BOLD + "%s" + RESET + COLOR_ERROR +
                    " cannot be removed from VPLS " + BOLD + "%s" + RESET + ".";

    protected Vpls vpls;
    protected InterfaceService interfaceService;

    @Argument(index = 0, name = "command", description = "Command name (add-if|" +
            "create|delete|list|rem-if|set-encap|show)",
            required = true, multiValued = false)
    @Completion(VplsCommandCompleter.class)
    String command = null;

    @Argument(index = 1, name = "vplsName", description = "The name of the VPLS",
            required = false, multiValued = false)
    @Completion(VplsNameCompleter.class)
    String vplsName = null;

    @Argument(index = 2, name = "optArg", description = "The interface name or" +
            " the encapsulation type for set-encap",
            required = false, multiValued = false)
    @Completion(VplsOptArgCompleter.class)
    String optArg = null;

    @Override
    protected void doExecute() {
        if (vpls == null) {
            vpls = get(Vpls.class);
        }
        if (interfaceService == null) {
            interfaceService = get(InterfaceService.class);
        }

        VplsCommandEnum enumCommand = VplsCommandEnum.enumFromString(command);
        if (enumCommand != null) {
            switch (enumCommand) {
                case ADD_IFACE:
                    addIface(vplsName, optArg);
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
                case CLEAN:
                    cleanVpls();
                    break;
                default:
                    print(VPLS_COMMAND_NOT_FOUND, command);
            }
        } else {
            print(VPLS_COMMAND_NOT_FOUND, command);
        }
    }

    /**
     * Adds an inteterface to a VPLS.
     *
     * @param vplsName the name of the VLPS
     * @param ifaceName the name of the interface to add
     */
    protected void addIface(String vplsName, String ifaceName) {
        if (vplsName == null) {
            print(INSERT_VPLS_NAME);
            return;
        }
        if (ifaceName == null) {
            print(INSERT_INTERFACE);
            return;
        }

        Interface iface = getInterface(ifaceName);
        VplsData vplsData = vpls.getVpls(vplsName);

        if (vplsData == null) {
            print(VPLS_NOT_FOUND, vplsName);
            return;
        }
        if (CHANGING_STATE.contains(vplsData.state())) {
            // when a VPLS is updating, we shouldn't try modify it.
            print("VPLS %s still updating, please wait it finished", vplsData.name());
            return;
        }
        if (iface == null) {
            print(IFACE_NOT_FOUND, ifaceName);
            return;
        }
        if (isIfaceAssociated(iface)) {
            print(IFACE_ALREADY_ASSOCIATED,
                  ifaceName, getVplsByInterface(iface).name());
            return;
        }
        vpls.addInterface(vplsData, iface);
    }

    /**
     * Creates a new VPLS.
     *
     * @param vplsName the name of the VLPS
     */
    protected void create(String vplsName) {
        if (vplsName == null || vplsName.isEmpty()) {
            print(INSERT_VPLS_NAME);
            return;
        }
        VplsData vplsData = vpls.getVpls(vplsName);
        if (vplsData != null) {
            print(VPLS_ALREADY_EXISTS, vplsName);
            return;
        }
        vpls.createVpls(vplsName, EncapsulationType.NONE);
    }

    /**
     * Deletes a VPLS.
     *
     * @param vplsName the name of the VLPS
     */
    protected void delete(String vplsName) {
        if (vplsName == null) {
            print(INSERT_VPLS_NAME);
            return;
        }
        VplsData vplsData = vpls.getVpls(vplsName);
        if (vplsData == null) {
            print(VPLS_NOT_FOUND, vplsName);
            return;
        }
        if (CHANGING_STATE.contains(vplsData.state())) {
            // when a VPLS is updating, we shouldn't try modify it.
            print("VPLS %s still updating, please wait it finished", vplsData.name());
            return;
        }
        vpls.removeVpls(vplsData);
    }

    /**
     * Lists the configured VPLSs.
     */
    protected void list() {
        List<String> vplsNames = vpls.getAllVpls().stream()
                .map(VplsData::name)
                .collect(Collectors.toList());
        Collections.sort(vplsNames);

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
    protected void removeIface(String vplsName, String ifaceName) {
        if (vplsName == null) {
            print(INSERT_VPLS_NAME);
            return;
        }
        if (ifaceName == null) {
            print(INSERT_INTERFACE);
            return;
        }
        VplsData vplsData = vpls.getVpls(vplsName);
        Interface iface = getInterface(ifaceName);
        if (vplsData == null) {
            print(VPLS_NOT_FOUND, vplsName);
            return;
        }
        if (CHANGING_STATE.contains(vplsData.state())) {
            // when a VPLS is updating, we shouldn't try modify it.
            print("VPLS %s still updating, please wait it finished", vplsData.name());
            return;
        }
        if (iface == null) {
            print(IFACE_NOT_FOUND, ifaceName);
            return;
        }
        if (!vplsData.interfaces().contains(iface)) {
            print(IFACE_NOT_ASSOCIATED, ifaceName, vplsName);
            return;
        }
        vpls.removeInterface(vplsData, iface);
    }

    /**
     * Sets the encapsulation type for a VPLS.
     *
     * @param vplsName the name of the VPLS
     * @param encap the encapsulation type
     */
    protected void setEncap(String vplsName, String encap) {
        if (vplsName == null) {
            print(INSERT_VPLS_NAME);
            return;
        }
        if (encap == null) {
            print(INSERT_ENCAP_TYPE);
            return;
        }
        VplsData vplsData = vpls.getVpls(vplsName);
        if (vplsData == null) {
            print(VPLS_NOT_FOUND, vplsName);
            return;
        }
        EncapsulationType encapType = EncapsulationType.enumFromString(encap);
        if (encapType.equals(EncapsulationType.NONE) &&
            !encapType.toString().equals(encap)) {
            print(ENCAP_NOT_FOUND, encap);
            return;
        }
        vpls.setEncapsulationType(vplsData, encapType);
    }

    /**
     * Shows the details of one or more VPLSs.
     *
     * @param vplsName the name of the VPLS
     */
    protected void show(String vplsName) {
        if (!isNullOrEmpty(vplsName)) {
            // A VPLS name is provided. Check first if the VPLS exists
            VplsData vplsData = vpls.getVpls(vplsName);
            if (vplsData != null) {
                Set<String> ifaceNames = vplsData.interfaces().stream()
                        .map(Interface::name)
                        .collect(Collectors.toSet());
                print(VPLS_DISPLAY,
                      vplsName,
                      ifaceNames,
                      vplsData.encapsulationType().toString(),
                      vplsData.state());
            } else {
                print(VPLS_NOT_FOUND, vplsName);
            }
        } else {
            Collection<VplsData> vplses = vpls.getAllVpls();
            // No VPLS names are provided. Display all VPLSs configured
            print(SEPARATOR);
            vplses.forEach(vplsData -> {
                Set<String> ifaceNames = vplsData.interfaces().stream()
                        .map(Interface::name)
                        .collect(Collectors.toSet());
                print(VPLS_DISPLAY,
                      vplsData.name(),
                      ifaceNames,
                      vplsData.encapsulationType().toString(),
                      vplsData.state());
                print(SEPARATOR);
            });
        }
    }

    /**
     * Remove all VPLS.
     */
    protected void cleanVpls() {
        vpls.removeAllVpls();
    }


    /**
     * States if an interface is already associated to a VPLS.
     *
     * @param iface the interface
     * @return true if the interface is already associated to a VPLS; false
     * otherwise
     */
    private boolean isIfaceAssociated(Interface iface) {
        return vpls.getAllVpls()
                .stream()
                .map(VplsData::interfaces)
                .flatMap(Collection::stream)
                .anyMatch(iface::equals);
    }

    /**
     * Gets a network interface by given interface name.
     *
     * @param interfaceName the interface name
     * @return the network interface
     */
    private Interface getInterface(String interfaceName) {
        // FIXME: only returns first interface it found
        // multiple interface with same name not support
        return interfaceService.getInterfaces().stream()
                .filter(iface -> iface.name().equals(interfaceName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets a VPLS related to the network interface.
     *
     * @param iface the network interface
     * @return the VPLS related to the network interface
     */
    private VplsData getVplsByInterface(Interface iface) {
        return vpls.getAllVpls().stream()
                .filter(vplsData -> vplsData.interfaces().contains(iface))
                .findFirst()
                .orElse(null);
    }
}
