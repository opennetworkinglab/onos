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
package org.onosproject.ospf.controller.impl;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.AdaptiveReceiveBufferSizePredictor;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.onosproject.net.driver.DriverService;
import org.onosproject.ospf.controller.OspfAgent;
import org.onosproject.ospf.controller.OspfArea;
import org.onosproject.ospf.controller.OspfInterface;
import org.onosproject.ospf.controller.OspfLinkTed;
import org.onosproject.ospf.controller.OspfProcess;
import org.onosproject.ospf.controller.OspfRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Representation of the main controller class. Handles all setup and network listeners.
 */
public class Controller {

    protected static final Logger log = LoggerFactory.getLogger(Controller.class);
    protected static final int BUFFER_SIZE = 4 * 1024 * 1024;
    private static final String PROCESS = "process";
    private static final String AREA = "area";
    private static final String INTERFACE = "interface";

    protected int ospfPort = 7000;
    protected int workerThreads = 16;
    protected long systemStartTime;
    private DriverService driverService;
    private OspfAgent agent;
    private List<ChannelGroup> cgList = new ArrayList();
    private List<NioServerSocketChannelFactory> execFactoryLst = new ArrayList<>();
    private List<OspfProcess> processes;

    /**
     * Gets all configured processes.
     *
     * @return all configured processes
     */
    public List<OspfProcess> getAllConfiguredProcesses() {
        return processes;
    }

    /**
     * Adds device details.
     *
     * @param ospfRouter OSPF router instance
     */
    public void addDeviceDetails(OspfRouter ospfRouter) {
        agent.addConnectedRouter(ospfRouter);
    }

    /**
     * Removes device details.
     *
     * @param ospfRouter OSPF router instance
     */
    public void removeDeviceDetails(OspfRouter ospfRouter) {
        agent.removeConnectedRouter(ospfRouter);
    }

    /**
     * Adds link details.
     *
     * @param ospfRouter  OSPF router instance
     * @param ospfLinkTed OSPF link ted instance
     */
    public void addLinkDetails(OspfRouter ospfRouter, OspfLinkTed ospfLinkTed) {
        agent.addLink(ospfRouter, ospfLinkTed);
    }

    /**
     * Removes link details.
     *
     * @param ospfRouter OSPF router instance
     */
    public void removeLinkDetails(OspfRouter ospfRouter) {
        agent.deleteLink(ospfRouter);
    }

    /**
     * Creates a server bootstrap.
     *
     * @return ServerBootstrap bootstrap instance
     */
    private ServerBootstrap createServerBootStrap() {

        Executor bossPool = Executors.newCachedThreadPool();
        Executor workerPool = Executors.newCachedThreadPool();
        NioServerSocketChannelFactory executerFactory;

        if (workerThreads == 0) {
            executerFactory = new NioServerSocketChannelFactory(bossPool, workerPool);
            execFactoryLst.add(executerFactory);
        } else {
            executerFactory = new NioServerSocketChannelFactory(bossPool, workerPool, workerThreads);
            execFactoryLst.add(executerFactory);
        }

        return new ServerBootstrap(executerFactory);
    }


    /**
     * Initializes internal data structures.
     */
    public void init() {
        this.systemStartTime = System.currentTimeMillis();
    }

    /**
     * Starts the controller.
     *
     * @param ag            OSPF agent instance
     * @param driverService driver service instance
     */
    public void start(OspfAgent ag, DriverService driverService) {
        log.info("Starting OSPF Controller...!!!");
        this.agent = ag;
        this.driverService = driverService;
        this.init();
    }

    /**
     * Stops the Controller.
     */
    public void stop() {
        log.info("Stopping OSPF Controller...!!!");

        for (ChannelGroup cg : cgList) {
            cg.close();
        }

        for (NioServerSocketChannelFactory execFactory : execFactoryLst) {
            execFactory.shutdown();
        }

        processes.clear();
    }

    /**
     * Deletes configured interface from the area.
     *
     * @param processId         process id
     * @param areaId            area id
     * @param interfaceToDelete interface to delete
     * @return true if operation success else false
     */
    public boolean deleteInterfaceFromArea(String processId, String areaId, String interfaceToDelete) {
        Iterator<OspfProcess> processItr = processes.iterator();

        while (processItr.hasNext()) {
            OspfProcess process = processItr.next();
            if (processId.equalsIgnoreCase(process.processId())) {
                Iterator<OspfArea> areaItr = process.areas().iterator();
                while (areaItr.hasNext()) {
                    OspfArea area = areaItr.next();
                    Iterator<OspfInterface> ospfIntrItr = area.getInterfacesLst().iterator();
                    if (area.areaId().toString().equalsIgnoreCase(areaId)) {
                        while (ospfIntrItr.hasNext()) {
                            OspfInterface ospfIntr = ospfIntrItr.next();
                            if (interfaceToDelete.equalsIgnoreCase(ospfIntr.ipAddress().toString())) {
                                ospfIntrItr.remove();
                                log.debug("Interface With Id {} is removed from Area {}",
                                          ospfIntr.ipAddress(), ospfIntr.areaId());
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    /*
    * Checks area with area id exists in process.
    *
    * @param processId process id
    * @param areaId area id
    * @return true if exist else false
    */
    public boolean checkArea(String processId, String areaId) {
        for (OspfProcess process : processes) {
            if (processId.equalsIgnoreCase(process.processId())) {
                for (OspfArea area : process.areas()) {
                    if (area.areaId().toString().equalsIgnoreCase(areaId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /*
    * Checks process with process id exists or not.
    *
    * @param processId process id
    * @return true if exist else false
    */
    public boolean checkProcess(String processId) {
        log.debug("CheckProcess,Process Id ={} processes={}", processId, processes);
        for (OspfProcess process : processes) {
            if (processId.equalsIgnoreCase(process.processId())) {
                return true;
            }
        }
        return false;
    }

    /*
    * Checks interface exists in given area.
    *
    * @param processId process id
    * @param areaId area id
    * @param ipAddress interface
    * @return true if exist else false
    */
    public boolean checkInterface(String processId, String areaId, String interfaceIp) {
        for (OspfProcess process : processes) {
            if (processId.equalsIgnoreCase(process.processId())) {
                for (OspfArea area : process.areas()) {
                    if (area.areaId().toString().equalsIgnoreCase(areaId)) {
                        for (OspfInterface ospfInterface : area.getInterfacesLst()) {
                            if (ospfInterface.ipAddress().toString().equalsIgnoreCase(interfaceIp)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Create processes first time when no process exist.
     * Create server bootstraps for all the interfaces in the processes.
     *
     * @param ospfProcesses list of OSPF processes to create
     */
    private void createProcessWhenNoProcessesExists(List<OspfProcess> ospfProcesses) {
        Set<String> interfaceIpList = new HashSet<>();
        Set<String> areaIdList = new HashSet<>();
        if (processes != null) {
            if (processes.size() == 0) {

                processes.addAll(ospfProcesses);
                for (OspfProcess process : ospfProcesses) {
                    for (OspfArea area : process.areas()) {
                        areaIdList.add(area.areaId().toString());
                        for (OspfInterface intrfc : area.getInterfacesLst()) {
                            interfaceIpList.add(intrfc.ipAddress().toString());
                        }
                    }
                }
                createBootStrapForCreatedInterface(interfaceIpList, areaIdList);
            }
        } else {
            processes = new ArrayList<>();
            processes.addAll(ospfProcesses);

            for (OspfProcess process : ospfProcesses) {
                for (OspfArea area : process.areas()) {
                    areaIdList.add(area.areaId().toString());
                    for (OspfInterface intrfc : area.getInterfacesLst()) {
                        interfaceIpList.add(intrfc.ipAddress().toString());
                    }
                }
            }
            createBootStrapForCreatedInterface(interfaceIpList, areaIdList);
        }
    }

    /**
     * Creates processes when already process exist.
     * It can be modifying existing process or adding a new process.
     *
     * @param ospfProcesses list of processes
     */
    private void createProcessWhenProcessesExists(List<OspfProcess> ospfProcesses) {
        if (ospfProcesses != null) {
            for (OspfProcess process : ospfProcesses) {
                if (!checkProcess(process.processId())) {
                    createNewProcess(process.processId(), process);
                } else {
                    List<OspfArea> areas = process.areas();
                    for (OspfArea area : areas) {
                        if (!checkArea(process.processId(), area.areaId().toString())) {
                            createAreaInProcess(process.processId(),
                                                area.areaId().toString(), area);
                        } else {
                            updateAreaInProcess(process.processId(), area.areaId().toString(), area);
                            for (OspfInterface interfc : area.getInterfacesLst()) {
                                if (!checkInterface(process.processId(),
                                                    area.areaId().toString(), interfc.ipAddress().toString())) {
                                    createInterfaceInAreaInProcess(process.processId(),
                                                                   area.areaId().toString(), interfc);
                                } else {
                                    updateInterfaceParameters(process.processId(),
                                                              area.areaId().toString(),
                                                              interfc.ipAddress().toString(), interfc);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Updates the area information in already started OSPF processes.
     *
     * @param processId     process id
     * @param areaId        area id
     * @param areaFrmConfig area to update
     */
    public void updateAreaInProcess(String processId, String areaId, OspfArea areaFrmConfig) {
        if (processes != null) {
            Iterator<OspfProcess> processItr = processes.iterator();
            while (processItr.hasNext()) {
                OspfProcess process = processItr.next();
                if (processId.equalsIgnoreCase(process.processId())) {
                    Iterator<OspfArea> area = process.areas().iterator();
                    while (area.hasNext()) {
                        OspfArea ospfArea = area.next();
                        if (areaId.equalsIgnoreCase(ospfArea.areaId().toString())) {
                            ospfArea.setAddressRanges(areaFrmConfig.addressRanges());
                            ospfArea.setRouterId(areaFrmConfig.routerId());
                            ospfArea.setTransitCapability(areaFrmConfig.isTransitCapability());
                            ospfArea.setExternalRoutingCapability(areaFrmConfig.isExternalRoutingCapability());
                            ospfArea.setStubCost(areaFrmConfig.stubCost());
                            ospfArea.setOptions(areaFrmConfig.options());
                            ospfArea.setIsOpaqueEnabled(areaFrmConfig.isOpaqueEnabled());
                            log.debug("updateAreaInProcess::Process Id::{}::Ospf Area with Id::{}::is " +
                                              "updated", processId, areaId);
                        }
                    }
                }
            }
        }
    }

    /**
     * Updates the processes configuration.
     *
     * @param ospfProcesses list of OSPF processes
     */
    public void updateConfig(List<OspfProcess> ospfProcesses) {
        log.info("Controller::UpdateConfig called");
        if (processes != null) {
            if (processes.size() == 0) {
                createProcessWhenNoProcessesExists(ospfProcesses);
            } else {
                createProcessWhenProcessesExists(ospfProcesses);
            }
        } else {
            createProcessWhenNoProcessesExists(ospfProcesses);
        }
    }

    /**
     * Deletes configuration.
     *
     * @param ospfProcesses OSPF processes
     * @param attribute     attribute to delete
     */
    public void deleteConfig(List<OspfProcess> ospfProcesses, String attribute) {
        log.info("Controller::UpdateConfig called");
        if (processes != null) {
            if (processes.size() == 0) {
                log.debug("DeleteConfig:: No process exists");
            } else {
                deleteProcessWhenExists(ospfProcesses, attribute);
            }
        } else {
            log.debug("DeleteConfig:: No process exists");
        }
    }

    /**
     * Creates a new process.
     *
     * @param processId process id
     * @param process   OSPF process instance
     */
    private void createNewProcess(String processId, OspfProcess process) {
        Set<String> interfaceIpList = new HashSet<>();
        Set<String> areaIdList = new HashSet<>();

        processes.add(process);
        for (OspfArea area : process.areas()) {
            areaIdList.add(area.areaId().toString());
            for (OspfInterface interfc : area.getInterfacesLst()) {
                interfaceIpList.add(interfc.ipAddress().toString());
            }
        }
        log.debug("createNewProcess::List of areas in process::{} areas::{}", processId, areaIdList);

        createBootStrapForCreatedInterface(interfaceIpList, areaIdList);

        log.debug("createNewProcess:: all processes::{}", processes);
    }

    /**
     * Creates a new area information in the process.
     *
     * @param processId process id
     * @param areaId    area id
     * @param area      OSPF area instance
     */
    private void createAreaInProcess(String processId, String areaId, OspfArea area) {
        Set<String> interfaceIpList = new HashSet<>();
        Set<String> areaIdList = new HashSet<>();

        Iterator<OspfProcess> processItr = processes.iterator();
        while (processItr.hasNext()) {
            OspfProcess process = processItr.next();
            List<OspfArea> areasInProcess = process.areas();
            if (processId.equalsIgnoreCase(process.processId())) {
                areasInProcess.add(area);

                for (OspfInterface intrfc : area.getInterfacesLst()) {
                    interfaceIpList.add(intrfc.ipAddress().toString());
                }
                areaIdList.add(area.areaId().toString());
                log.debug("createAreaInProcess::List of areas in process Id::{} " +
                                  "AreaId ::{} update process::{}",
                          processId, areaId, process);
            }
        }
        createBootStrapForCreatedInterface(interfaceIpList, areaIdList);
        log.debug("createAreaInProcess:: all processes::{}", processes);
    }

    /**
     * Creates an interface in the given area and process.
     *
     * @param processId     process id
     * @param areaId        area id
     * @param ospfInterface OSPF interface instance
     */
    private void createInterfaceInAreaInProcess(String processId,
                                                String areaId, OspfInterface ospfInterface) {
        Set<String> interfaceIpList = new HashSet<>();
        Set<String> areaIdList = new HashSet<>();

        Iterator<OspfProcess> processItr = processes.iterator();
        while (processItr.hasNext()) {
            OspfProcess process = processItr.next();
            List<OspfArea> areasInProcess = process.areas();
            if (processId.equalsIgnoreCase(process.processId())) {
                Iterator<OspfArea> areaItr = areasInProcess.iterator();
                while (areaItr.hasNext()) {
                    OspfArea area = areaItr.next();
                    if (areaId.equalsIgnoreCase(area.areaId().toString())) {
                        area.getInterfacesLst().add(ospfInterface);
                        interfaceIpList.add(ospfInterface.ipAddress().toString());

                        log.debug("createInterfaceInAreaInProcess::Interface " +
                                          "updated in process Id::{} AreaId ::{} Interface List{}",
                                  processId, areaId, area.getInterfacesLst());

                    }
                }
            }
        }
        createBootStrapForCreatedInterface(interfaceIpList, areaIdList);
        log.debug("createInterfaceInAreaInProcess:: all processes::{}", processes);
    }

    /**
     * Updates interface parameters.
     *
     * @param processId     process id
     * @param areaId        area id
     * @param interfaceId   interface id
     * @param ospfInterface OSPF interface instance
     */
    private void updateInterfaceParameters(String processId, String areaId, String interfaceId,
                                           OspfInterface ospfInterface) {
        Iterator<OspfProcess> processItr = processes.iterator();
        while (processItr.hasNext()) {
            OspfProcess process = processItr.next();
            if (processId.equalsIgnoreCase(process.processId())) {
                Iterator<OspfArea> areItr = process.areas().iterator();
                while (areItr.hasNext()) {
                    OspfArea area = (OspfArea) areItr.next();
                    if (area.areaId().toString().equalsIgnoreCase(areaId)) {
                        Iterator<OspfInterface> intfcList = area.getInterfacesLst().iterator();
                        while (intfcList.hasNext()) {
                            OspfInterface intrfcObj = intfcList.next();
                            if (interfaceId.equalsIgnoreCase(intrfcObj.ipAddress().toString())) {
                                intrfcObj.setPollInterval(ospfInterface.pollInterval());
                                intrfcObj.setTransmitDelay(ospfInterface.transmitDelay());
                                intrfcObj.setBdr(ospfInterface.bdr());
                                intrfcObj.setDr(ospfInterface.dr());
                                intrfcObj.setAuthKey(ospfInterface.authKey());
                                intrfcObj.setAuthType(ospfInterface.authType());
                                intrfcObj.setHelloIntervalTime(ospfInterface.helloIntervalTime());
                                intrfcObj.setReTransmitInterval(ospfInterface.reTransmitInterval());
                                intrfcObj.setMtu(ospfInterface.mtu());
                                intrfcObj.setInterfaceCost(ospfInterface.interfaceCost());
                                intrfcObj.setInterfaceType(ospfInterface.interfaceType());
                                intrfcObj.setRouterDeadIntervalTime(ospfInterface.routerDeadIntervalTime());
                                intrfcObj.setRouterPriority(ospfInterface.routerPriority());
                                intrfcObj.setIpNetworkMask(ospfInterface.ipNetworkMask());
                                log.debug("updateInterfaceParameters::Interface updated in " +
                                                  "process Id::{} AreaId ::{} Interface Id:{} " +
                                                  "Updated Interface List: {}", processId, areaId,
                                          interfaceId, intfcList);
                            }
                        }
                    }
                }
            }
        }
        log.debug("updateInterfaceParameters:: all processes::{}", processes);
    }

    /**
     * Creates server bootstrap for interface.
     *
     * @param interfaceIPs set of interfaces
     * @param areaIds      set of area id's
     */
    private void createBootStrapForCreatedInterface(Set<String> interfaceIPs, Set<String> areaIds) {

        log.debug("createBootStrapForCreatedInterface:: List of new Interfaces::{}, " +
                          "List of new areas::{}", interfaceIPs, areaIds);
        List<String> networkInterfaces = new ArrayList();
        //get the connected interfaces
        Enumeration<NetworkInterface> nets = null;
        try {
            nets = NetworkInterface.getNetworkInterfaces();
            // Check NetworkInterfaces and add the IP's
            for (NetworkInterface netInt : Collections.list(nets)) {
                // if the interface is up & not loopback
                if (!netInt.isUp() && !netInt.isLoopback()) {
                    continue;
                }
                //get all the InetAddresses
                Enumeration<InetAddress> inetAddresses = netInt.getInetAddresses();
                for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                    String ipAddress = inetAddress.getHostAddress();
                    networkInterfaces.add(ipAddress);
                }
            }
            //Search for the address in all configured areas interfaces
            for (OspfProcess process : processes) {
                for (OspfArea area : process.areas()) {
                    for (OspfInterface ospfIf : area.getInterfacesLst()) {
                        String ipFromConfig = ospfIf.ipAddress().toString();
                        if (interfaceIPs.contains(ipFromConfig)) {
                            log.debug("Ip address::{} for area {} is newly created" + ipFromConfig);
                            if (networkInterfaces.contains(ipFromConfig)) {
                                log.debug("Both Config and Interface have ipAddress {} for area {}",
                                          ipFromConfig, area.areaId());
                                // if same IP address create
                                try {
                                    log.debug("Creating ServerBootstrap for {} @ {}", ipFromConfig, ospfPort);

                                    final ServerBootstrap bootstrap = createServerBootStrap();

                                    bootstrap.setOption("receiveBufferSize", Controller.BUFFER_SIZE);
                                    bootstrap.setOption("receiveBufferSizePredictorFactory",
                                                        new FixedReceiveBufferSizePredictorFactory(
                                                                Controller.BUFFER_SIZE));
                                    bootstrap.setOption("reuseAddress", true);
                                    bootstrap.setOption("tcpNoDelay", true);
                                    bootstrap.setOption("keepAlive", true);

                                    bootstrap.setOption("child.receiveBufferSize", Controller.BUFFER_SIZE);
                                    bootstrap.setOption("child.receiveBufferSizePredictorFactory",
                                                        new FixedReceiveBufferSizePredictorFactory(
                                                                Controller.BUFFER_SIZE));
                                    bootstrap.setOption("child.reuseAddress", true);
                                    bootstrap.setOption("child.tcpNoDelay", true);
                                    bootstrap.setOption("child.keepAlive", true);
                                    bootstrap.setOption("receiveBufferSizePredictorFactory",
                                                        new FixedReceiveBufferSizePredictorFactory(
                                                                Controller.BUFFER_SIZE));
                                    bootstrap.setOption("receiveBufferSizePredictor",
                                                        new AdaptiveReceiveBufferSizePredictor(64, 1024, 65536));

                                    ChannelPipelineFactory pfact = new OspfPipelineFactory(this, area, ospfIf);
                                    bootstrap.setPipelineFactory(pfact);
                                    InetSocketAddress sa = new InetSocketAddress(InetAddress.getByName(ipFromConfig),
                                                                                 ospfPort);

                                    ChannelGroup cg = new DefaultChannelGroup();
                                    cg.add(bootstrap.bind(sa));
                                    cgList.add(cg);

                                    log.debug("Listening for connections on {}", sa);

                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        } else {
                            log.debug("Ip address::{} for area {} is not newly created" + ipFromConfig);
                        }
                    }
                    if (areaIds.contains(area.areaId().toString())) {
                        area.initializeDb();
                    }
                }
            }

        } catch (SocketException e) {
            log.error("Error occured due to SocketException::Class::{},Line::{},Method::{}",
                      e.getStackTrace()[0].getFileName(), e.getStackTrace()[0].getLineNumber(),
                      e.getStackTrace()[0].getMethodName());
        }
    }

    /**
     * Deletes given process.
     *
     * @param ospfProcesses list of OSPF process instance.
     * @param attribute     attribute to delete
     */
    public void deleteProcessWhenExists(List<OspfProcess> ospfProcesses, String attribute) {
        if (ospfProcesses != null) {
            for (OspfProcess process : ospfProcesses) {
                if (checkProcess(process.processId())) {
                    if (PROCESS.equalsIgnoreCase(attribute)) {
                        deleteProcess(process.processId(), process);
                    } else {
                        List<OspfArea> areas = process.areas();
                        for (OspfArea area : areas) {
                            if (checkArea(process.processId(), area.areaId().toString())) {
                                if (AREA.equalsIgnoreCase(attribute)) {
                                    deleteAreaFromProcess(process.processId(),
                                                          area.areaId().toString(), area);
                                } else {
                                    for (OspfInterface interfc : area.getInterfacesLst()) {
                                        if (checkInterface(process.processId(),
                                                           area.areaId().toString(),
                                                           interfc.ipAddress().toString())) {
                                            if (INTERFACE.equalsIgnoreCase(attribute)) {
                                                deleteInterfaceFromAreaProcess(process.processId(),
                                                                               area.areaId().toString(),
                                                                               interfc);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Deletes given process.
     *
     * @param processId process id
     * @param process   OSPF process instance
     */
    private void deleteProcess(String processId, OspfProcess process) {
        if (processes != null) {
            Iterator<OspfProcess> itrProcess = processes.iterator();
            while (itrProcess.hasNext()) {
                OspfProcess ospfPrs = itrProcess.next();
                if (processId.equalsIgnoreCase(ospfPrs.processId())) {
                    itrProcess.remove();
                }
            }
        }
    }

    /**
     * Deletes area from process.
     *
     * @param processId process id
     * @param areaId    area id
     * @param area      OSPF area instance
     */
    private void deleteAreaFromProcess(String processId, String areaId, OspfArea area) {
        if (processes != null) {
            Iterator<OspfProcess> itrProcess = processes.iterator();
            while (itrProcess.hasNext()) {
                OspfProcess ospfPrs = itrProcess.next();
                if (processId.equalsIgnoreCase(ospfPrs.processId())) {
                    if (ospfPrs.areas() != null) {
                        Iterator<OspfArea> itrArea = ospfPrs.areas().iterator();
                        while (itrArea.hasNext()) {
                            OspfArea ospfArea = itrArea.next();
                            if (areaId.equalsIgnoreCase(ospfArea.areaId().toString())) {
                                itrArea.remove();
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Deletes interface from area.
     *
     * @param processId         process id
     * @param areaId            area id
     * @param interfaceToDelete interface to delete
     */
    private void deleteInterfaceFromAreaProcess(String processId, String areaId, OspfInterface interfaceToDelete) {
        if (processes != null) {
            Iterator<OspfProcess> itrProcess = processes.iterator();
            while (itrProcess.hasNext()) {
                OspfProcess ospfPrs = itrProcess.next();
                if (processId.equalsIgnoreCase(ospfPrs.processId())) {
                    if (ospfPrs.areas() != null) {
                        Iterator<OspfArea> itrArea = ospfPrs.areas().iterator();
                        while (itrArea.hasNext()) {
                            OspfArea ospfArea = itrArea.next();
                            if (areaId.equalsIgnoreCase(ospfArea.areaId().toString())) {
                                if (ospfArea.getInterfacesLst() != null) {
                                    Iterator<OspfInterface> intrfcList = ospfArea.getInterfacesLst().iterator();
                                    while (intrfcList.hasNext()) {
                                        OspfInterface ospfItrfc = intrfcList.next();
                                        if (interfaceToDelete.ipAddress().equals(ospfItrfc.ipAddress())) {
                                            intrfcList.remove();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}