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
package org.onosproject.isis.cli;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.MacAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.isis.controller.IsisController;
import org.onosproject.isis.controller.IsisInterface;
import org.onosproject.isis.controller.IsisLsdb;
import org.onosproject.isis.controller.IsisNeighbor;
import org.onosproject.isis.controller.IsisNetworkType;
import org.onosproject.isis.controller.IsisProcess;
import org.onosproject.isis.controller.IsisRouterType;
import org.onosproject.isis.controller.LspWrapper;
import org.onosproject.isis.io.isispacket.pdu.LsPdu;
import org.onosproject.isis.io.util.IsisConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.NetworkInterface;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lists ISIS neighbors, database and interfaces details.
 */
@Command(scope = "onos", name = "isis", description = "lists database, neighbors and interfaces")
public class ApplicationIsisCommand extends AbstractShellCommand {
    private static final Logger log = LoggerFactory.getLogger(ApplicationIsisCommand.class);
    private static final String INTERFACE = "interface";
    private static final String DATABASE = "database";
    private static final String NEIGHBOR = "neighbor";
    private static final String P2P = "P2P";
    private static final String LAN = "LAN";
    private static final String L1 = "L1";
    private static final String L2 = "L2";
    private static final String L1L2 = "L1L2";
    protected IsisController isisController;
    @Argument(index = 0, name = "name",
            description = "interface|database|neighbor",
            required = true, multiValued = false)
    String name = null;
    @Argument(index = 1, name = "processId",
            description = "processId", required = true, multiValued = false)
    String processId = null;

    @Activate
    public void activate() {
        log.debug("Activated...!!!");
    }

    @Deactivate
    public void deactivate() {
        log.debug("Deactivated...!!!");
    }

    @Override
    protected void execute() {
        switch (name) {
            case INTERFACE:
                displayIsisInterfaces();
                break;
            case NEIGHBOR:
                displayIsisNeighbors();
                break;
            case DATABASE:
                displayIsisDatabase();
                break;
            default:
                log.debug("Unknown command...!!");
                break;
        }
    }

    /**
     * Displays ISIS neighbor details.
     */
    private void displayIsisNeighbors() {
        String interfaceName = "";
        String circuitType = "";
        boolean invalidProcess = true;
        try {
            this.isisController = get(IsisController.class);
            List<IsisProcess> listOfProcess = isisController.allConfiguredProcesses();
            if (listOfProcess == null) {
                return;
            }
            displayNeighborHeader();
            Iterator<IsisProcess> itrProcess = listOfProcess.iterator();
            while (itrProcess.hasNext()) {
                IsisProcess isisProcess = itrProcess.next();
                if (processId != null && processId.trim().equals(isisProcess.processId())) {
                    invalidProcess = false;
                    List<IsisInterface> interfaceList = isisProcess.isisInterfaceList();
                    Iterator<IsisInterface> itrInterface = interfaceList.iterator();
                    while (itrInterface.hasNext()) {
                        IsisInterface isisInterface = itrInterface.next();
                        interfaceName = NetworkInterface.getByIndex(isisInterface.interfaceIndex()).getName();
                        Set<MacAddress> getNeighborList = isisInterface.neighbors();
                        for (MacAddress mac : getNeighborList) {
                            IsisNeighbor neighbor = isisInterface.lookup(mac);
                            switch (neighbor.routerType()) {
                                case L1:
                                    circuitType = L1;
                                    break;
                                case L2:
                                    circuitType = L2;
                                    break;
                                case L1L2:
                                    circuitType = L1L2;
                                    break;
                                default:
                                    log.debug("Unknown circuit type...!!");
                                    break;
                            }
                            print("%-20s%-20s%-20s%-20s%-20s%-20s\n", neighbor.neighborSystemId(),
                                  neighbor.neighborMacAddress().toString(), interfaceName,
                                  circuitType, neighbor.interfaceState(), neighbor.holdingTime());
                        }
                    }
                }
            }
            if (invalidProcess) {
                print("%s\n", "Process " + processId + " not exist...!!!");
            }
        } catch (Exception e) {
            log.debug("Error occurred while displaying ISIS neighbor: {}", e.getMessage());
        }
    }

    /**
     * Displays ISIS database details.
     */
    private void displayIsisDatabase() {
        try {
            this.isisController = get(IsisController.class);
            List<IsisProcess> listOfProcess = isisController.allConfiguredProcesses();
            Iterator<IsisProcess> itrProcess = listOfProcess.iterator();
            boolean invalidProcess = true;
            while (itrProcess.hasNext()) {
                IsisProcess isisProcess = itrProcess.next();
                if (processId != null && processId.trim().equals(isisProcess.processId())) {
                    invalidProcess = false;
                    List<IsisInterface> interfaceList = isisProcess.isisInterfaceList();
                    Iterator<IsisInterface> itrInterface = interfaceList.iterator();
                    if (itrInterface.hasNext()) {
                        IsisInterface isisInterface = itrInterface.next();
                        IsisLsdb isisLsdb = isisInterface.isisLsdb();
                        if (isisLsdb != null) {
                            Map<String, LspWrapper> lsWrapperListL1 = isisLsdb.getL1Db();
                            Map<String, LspWrapper> lsWrapperListL2 = isisLsdb.getL2Db();
                            Set<String> l1Wrapper = lsWrapperListL1.keySet();
                            Set<String> l2Wrapper = lsWrapperListL2.keySet();
                            if (l1Wrapper.size() > 0) {
                                print("IS-IS Level-1 link-state database:");
                                displayDatabaseHeader();
                                for (String string : l1Wrapper) {
                                    LspWrapper lspWrapper = lsWrapperListL1.get(string);
                                    LsPdu lsPdu = (LsPdu) lspWrapper.lsPdu();
                                    print("%-25s%-25s%-25s%-25s%-25s\n", lsPdu.lspId(), lsPdu.pduLength(),
                                          lsPdu.sequenceNumber(),
                                          Integer.toHexString(lsPdu.checkSum()), lspWrapper.remainingLifetime());
                                }
                            }
                            if (l2Wrapper.size() > 0) {
                                print("IS-IS Level-2 link-state database:");
                                displayDatabaseHeader();
                                for (String string2 : l2Wrapper) {
                                    LspWrapper lspWrapper2 = lsWrapperListL2.get(string2);
                                    LsPdu lsPdu2 = (LsPdu) lspWrapper2.lsPdu();
                                    print("%-25s%-25s%-25s%-25s%-25s\n", lsPdu2.lspId(), lsPdu2.pduLength(),
                                          lsPdu2.sequenceNumber(), Integer.toHexString(lsPdu2.checkSum()),
                                          IsisConstants.LSPMAXAGE - lspWrapper2.currentAge());
                                }
                            }
                            break;
                        }
                    }
                }
            }
            if (invalidProcess) {
                print("%s\n", "Process " + processId + " not exist...!!!");
            }
        } catch (Exception e) {
            log.debug("Error occurred while displaying ISIS database: {}", e.getMessage());
        }
    }

    /**
     * Displays ISIS interfaces.
     */
    private void displayIsisInterfaces() {
        String interfaceName = "";
        String networkType = "";
        String circuitType = "";
        boolean invalidProcess = true;
        try {
            this.isisController = get(IsisController.class);
            List<IsisProcess> listOfProcess = isisController.allConfiguredProcesses();
            if (listOfProcess == null) {
                return;
            }
            displayInterfaceHeader();
            Iterator<IsisProcess> itrProcess = listOfProcess.iterator();
            while (itrProcess.hasNext()) {
                IsisProcess isisProcess = itrProcess.next();
                if (processId != null && processId.trim().equals(isisProcess.processId())) {
                    invalidProcess = false;
                    List<IsisInterface> interfaceList = isisProcess.isisInterfaceList();
                    for (IsisInterface isisInterface : interfaceList) {

                        if (isisInterface.networkType() == IsisNetworkType.P2P) {
                            networkType = P2P;
                        } else {
                            networkType = LAN;
                        }

                        switch (IsisRouterType.get(isisInterface.reservedPacketCircuitType())) {
                            case L1:
                                circuitType = L1;
                                break;
                            case L2:
                                circuitType = L2;
                                break;
                            case L1L2:
                                circuitType = L1L2;
                                break;
                            default:
                                log.debug("Unknown circuit type...!!");
                                break;
                        }
                        interfaceName = NetworkInterface.getByIndex(isisInterface.interfaceIndex()).getName();
                        print("%-20s%-20s%-20s%-20s\n", interfaceName, isisInterface.areaAddress(),
                              networkType, circuitType);
                    }
                }
            }
            if (invalidProcess) {
                print("%s\n", "Process " + processId + " not exist...!!!");
            }
        } catch (Exception e) {
            log.debug("Error occurred while displaying ISIS interface: {}", e.getMessage());
        }
    }

    /**
     * Displays ISIS interface header.
     */
    private void displayInterfaceHeader() {
        print("%-20s%-20s%-20s%-20s\n", "Interface", "Area Id", "TYPE", "Level");
    }

    /**
     * Displays ISIS neighbor header.
     */
    private void displayNeighborHeader() {
        print("%-20s%-20s%-20s%-20s%-20s%-20s\n", "System Id", "Mac Id", "Interface",
              "Level", "State", "Holding Time");
    }

    /**
     * Displays ISIS database header.
     */
    private void displayDatabaseHeader() {
        print("%-25s%-25s%-25s%-25s%-25s\n", "LSP ID ", "PduLen", "SeqNumber", "Checksum",
              "Remaining Life Time");
    }
}
