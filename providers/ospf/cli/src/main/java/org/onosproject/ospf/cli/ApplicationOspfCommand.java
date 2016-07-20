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
package org.onosproject.ospf.cli;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.ospf.controller.OspfArea;
import org.onosproject.ospf.controller.OspfController;
import org.onosproject.ospf.controller.OspfInterface;
import org.onosproject.ospf.controller.OspfLsaType;
import org.onosproject.ospf.controller.OspfNbr;
import org.onosproject.ospf.controller.OspfProcess;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.types.RouterLsa;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Representation of OSPF cli commands.
 */
@Component(immediate = true)
@Command(scope = "onos", name = "ospf", description = "list database")
public class ApplicationOspfCommand extends AbstractShellCommand {

    protected static final String FORMAT6 = "%-20s%-20s%-20s%-20s%-20s%-20s\n";
    protected static final String FORMAT5 = "%-20s%-20s%-20s%-20s%-20s\n";
    protected static final String NETWORK = "NETWORK";
    protected static final String SUMMARY = "SUMMARY";
    protected static final String ASBR = "ABSR";
    protected static final String EXTERNAL = "EXTERNAL";
    protected static final String LINKLOOPAQ = "LINKLOCALOPAQUE";
    protected static final String AREALOCOPAQ = "AREALOCALOPAQUE";
    protected static final String ASOPAQ = "ASOPAQUE";
    protected static final String DR = "DR";
    protected static final String BACKUP = "BACKUP";
    protected static final String DROTHER = "DROther";
    static final String DATABASE = "database";
    static final String NEIGHBORLIST = "neighbors";
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OspfController ospfController;
    @Argument(index = 0, name = "name",
            description = "database|neighborlist",
            required = true, multiValued = false)
    private String name = null;
    @Argument(index = 1, name = "processid",
            description = "processId",
            required = true, multiValued = false)
    private String process = null;
    @Argument(index = 2, name = "areaid",
            description = "areaId",
            required = false, multiValued = false)
    private String area = null;
    private List<String> routerLsa = new ArrayList<>();
    private List<String> networkLsa = new ArrayList<>();
    private List<String> summaryLsa = new ArrayList<>();
    private List<String> externalLsa = new ArrayList<>();
    private List<String> asbrSumm = new ArrayList<>();
    private List<String> areaLocalOpaqLsa = new ArrayList<>();
    private List<String> linkLocalOpqLsa = new ArrayList<>();
    private List<String> asOpqLsa = new ArrayList<>();
    private List<String> undefinedLsa = new ArrayList<>();
    private List<OspfArea> areaList = new ArrayList<>();


    @Activate
    public void activate() {
        print("OSPF cli activated...!!!");
        log.debug("OSPF cli activated...!!!");
    }

    @Deactivate
    public void deactivate() {
        log.debug("OSPF cli deactivated...!!!");
    }

    @Override
    protected void execute() {
        if (DATABASE.equals(name)) {
            buildOspfDatabaseInformation();
        } else if (NEIGHBORLIST.equals(name)) {
            buildNeighborInformation();
        } else {
            print("Please check the command (database|neighbor)");
        }
    }

    /**
     * Clears all the lists.
     */
    private void clearLists() {
        routerLsa.clear();
        networkLsa.clear();
        summaryLsa.clear();
        externalLsa.clear();
        asbrSumm.clear();
        areaLocalOpaqLsa.clear();
        linkLocalOpqLsa.clear();
        asOpqLsa.clear();
        undefinedLsa.clear();
        areaList.clear();
    }

    /**
     * Builds OSPF database information.
     */
    private void buildOspfDatabaseInformation() {
        try {
            //Builds LSA details
            buildLsaLists();
            for (OspfArea area : areaList) {
                if (routerLsa.size() > 0) {
                    printRouterFormat(area.areaId().toString(), area.routerId().toString(), process);
                    for (String str : routerLsa) {
                        String[] lsaVal = str.split("\\,");
                        if (area.areaId().toString().equalsIgnoreCase(lsaVal[0])) {
                            print(FORMAT6, lsaVal[2], lsaVal[3], lsaVal[4], lsaVal[5], lsaVal[6], lsaVal[7]);
                        }
                    }
                }
                if (networkLsa.size() > 0) {
                    printNetworkFormat(area.areaId().toString(), NETWORK);
                    printDetails(networkLsa, area.areaId().toString());
                }
                if (summaryLsa.size() > 0) {
                    printNetworkFormat(area.areaId().toString(), SUMMARY);
                    printDetails(summaryLsa, area.areaId().toString());
                }
                if (externalLsa.size() > 0) {
                    printNetworkFormat(area.areaId().toString(), EXTERNAL);
                    printDetails(externalLsa, area.areaId().toString());
                }
                if (asbrSumm.size() > 0) {
                    printNetworkFormat(area.areaId().toString(), ASBR);
                    printDetails(asbrSumm, area.areaId().toString());
                }
                if (areaLocalOpaqLsa.size() > 0) {
                    printNetworkFormat(area.areaId().toString(), AREALOCOPAQ);
                    printDetails(areaLocalOpaqLsa, area.areaId().toString());
                }
                if (linkLocalOpqLsa.size() > 0) {
                    printNetworkFormat(area.areaId().toString(), LINKLOOPAQ);
                    printDetails(linkLocalOpqLsa, area.areaId().toString());
                }
                if (asOpqLsa.size() > 0) {
                    printNetworkFormat(area.areaId().toString(), ASOPAQ);
                    printDetails(asOpqLsa, area.areaId().toString());
                }
                if (undefinedLsa.size() > 0) {
                    printRouterFormat(area.areaId().toString(), area.routerId().toString(), process);
                    printDetails(undefinedLsa, area.areaId().toString());
                }
            }
            clearLists();
        } catch (Exception ex) {
            clearLists();
            print("Error occured while Ospf controller getting called" + ex.getMessage());
        }
    }

    /**
     * Prints LSA details.
     *
     * @param lsaDetails LSA details
     * @param areaId     area ID
     */
    private void printDetails(List<String> lsaDetails, String areaId) {
        for (String str : lsaDetails) {
            String[] lsaVal = str.split("\\,");
            if (areaId.equalsIgnoreCase(lsaVal[0])) {
                print(FORMAT5, lsaVal[2], lsaVal[3], lsaVal[4], lsaVal[5], lsaVal[6]);
            }
        }
    }

    /**
     * Builds all LSA lists with LSA details.
     */
    private void buildLsaLists() {
        this.ospfController = get(OspfController.class);
        List<OspfProcess> listOfProcess = ospfController.getAllConfiguredProcesses();
        Iterator<OspfProcess> itrProcess = listOfProcess.iterator();
        while (itrProcess.hasNext()) {
            OspfProcess ospfProcess = itrProcess.next();
            if (process.equalsIgnoreCase(ospfProcess.processId())) {
                List<OspfArea> listAreas = ospfProcess.areas();
                Iterator<OspfArea> itrArea = listAreas.iterator();
                while (itrArea.hasNext()) {
                    OspfArea area = itrArea.next();
                    List<LsaHeader> lsas = area.database()
                            .getAllLsaHeaders(false, area.isOpaqueEnabled());
                    List<LsaHeader> tmpLsaList = new ArrayList<>(lsas);
                    log.debug("OSPFController::size of database::" + (lsas != null ? lsas.size() : null));
                    Iterator<LsaHeader> itrLsaHeader = tmpLsaList.iterator();
                    areaList.add(area);
                    if (itrLsaHeader != null) {
                        while (itrLsaHeader.hasNext()) {
                            LsaHeader header = itrLsaHeader.next();
                            populateLsaLists(header, area);
                        }
                    }
                }
            }
        }
    }

    /**
     * Populates the LSA lists based on the input.
     *
     * @param header LSA header instance
     * @param area   OSPF area instance
     */
    private void populateLsaLists(LsaHeader header, OspfArea area) {
        String seqNo = Long.toHexString(header.lsSequenceNo());
        String checkSum = Long.toHexString(header.lsCheckSum());
        if (seqNo.length() == 16) {
            seqNo = seqNo.substring(8, seqNo.length());
        }
        if (checkSum.length() == 16) {
            checkSum = checkSum.substring(8, checkSum.length());
        }
        StringBuffer strBuf = getBuffList(area.areaId().toString(), area.routerId().toString(),
                                          header.linkStateId(),
                                          header.advertisingRouter().toString(),
                                          header.age(), seqNo, checkSum);
        if (header.lsType() == OspfLsaType.ROUTER.value()) {
            strBuf.append(",");
            strBuf.append(((RouterLsa) header).noLink());
            routerLsa.add(strBuf.toString());
        } else if (header.lsType() == OspfLsaType.NETWORK.value()) {
            strBuf.append(",");
            strBuf.append("0");
            networkLsa.add(strBuf.toString());
        } else if (header.lsType() == OspfLsaType.SUMMARY.value()) {
            strBuf.append(",");
            strBuf.append("0");
            summaryLsa.add(strBuf.toString());
        } else if (header.lsType() == OspfLsaType.EXTERNAL_LSA.value()) {
            strBuf.append(",");
            strBuf.append("0");
            externalLsa.add(strBuf.toString());
        } else if (header.lsType() == OspfLsaType.ASBR_SUMMARY.value()) {
            strBuf.append(",");
            strBuf.append("0");
            asbrSumm.add(strBuf.toString());
        } else if (header.lsType() == OspfLsaType.AREA_LOCAL_OPAQUE_LSA.value()) {
            strBuf.append(",");
            strBuf.append("0");
            areaLocalOpaqLsa.add(strBuf.toString());
        } else if (header.lsType() == OspfLsaType.LINK_LOCAL_OPAQUE_LSA.value()) {
            strBuf.append(",");
            strBuf.append("0");
            linkLocalOpqLsa.add(strBuf.toString());
        } else if (header.lsType() == OspfLsaType.AS_OPAQUE_LSA.value()) {
            strBuf.append(",");
            strBuf.append("0");
            asOpqLsa.add(strBuf.toString());
        } else {
            strBuf.append(",");
            strBuf.append("0");
            undefinedLsa.add(strBuf.toString());
        }
    }

    /**
     * Builds OSPF neighbor information.
     */
    private void buildNeighborInformation() {
        try {
            this.ospfController = get(OspfController.class);
            List<OspfProcess> listOfProcess = ospfController.getAllConfiguredProcesses();
            boolean flag = false;
            printNeighborsFormat();
            Iterator<OspfProcess> itrProcess = listOfProcess.iterator();
            while (itrProcess.hasNext()) {
                OspfProcess process = itrProcess.next();
                List<OspfArea> listAreas = process.areas();
                Iterator<OspfArea> itrArea = listAreas.iterator();
                while (itrArea.hasNext()) {
                    OspfArea area = itrArea.next();
                    List<OspfInterface> itrefaceList = area.ospfInterfaceList();
                    for (OspfInterface interfc : itrefaceList) {
                        List<OspfNbr> nghbrList = new ArrayList<>(interfc.listOfNeighbors().values());
                        for (OspfNbr neigbor : nghbrList) {
                            print("%-20s%-20s%-20s%-20s%-20s\n", neigbor.neighborId(), neigbor.routerPriority(),
                                  neigbor.getState() + "/" + checkDrBdrOther(neigbor.neighborIpAddr().toString(),
                                                                             neigbor.neighborDr().toString(),
                                                                             neigbor.neighborBdr().toString()),
                                  neigbor.neighborIpAddr().toString(), interfc.ipAddress());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            print("Error occured while Ospf controller getting called" + ex.getMessage());
        }
    }

    /**
     * Prints input after formatting.
     *
     * @param areaId    area ID
     * @param routerId  router ID
     * @param processId process ID
     */
    private void printRouterFormat(String areaId, String routerId, String processId) {
        print("%s (%s) %s %s\n", "OSPF Router with ID", routerId, "Process Id", processId);
        print("%s ( Area %s)\n", "Router Link States", areaId);
        print("%-20s%-20s%-20s%-20s%-20s%-20s\n", "Link Id", "ADV Router", "Age", "Seq#",
              "CkSum", "Link Count");
    }

    /**
     * Prints input after formatting.
     *
     * @param areaId area ID
     * @param type   network type
     */
    private void printNetworkFormat(String areaId, String type) {
        print("%s %s ( Area %s)\n", type, "Link States", areaId);
        print("%-20s%-20s%-20s%-20s%-20s\n", "Link Id", "ADV Router", "Age", "Seq#", "CkSum");
    }

    /**
     * Prints input after formatting.
     */
    private void printNeighborsFormat() {
        print("%-20s%-20s%-20s%-20s%-20s\n", "Neighbor Id", "Pri", "State",
              "Address", "Interface");
    }

    /**
     * Checks whether the neighbor is DR or BDR.
     *
     * @param ip    IP address to check
     * @param drIP  DRs IP address
     * @param bdrIp BDRs IP address
     * @return 1- neighbor is DR, 2- neighbor is BDR, 3- DROTHER
     */
    public String checkDrBdrOther(String ip, String drIP, String bdrIp) {

        if (ip.equalsIgnoreCase(drIP)) {
            return DR;
        } else if (ip.equalsIgnoreCase(bdrIp)) {
            return BACKUP;
        } else {
            return DROTHER;
        }
    }

    /**
     * Returns inputs as formatted string.
     *
     * @param areaId            area id
     * @param routerId          router id
     * @param linkStateId       link state id
     * @param advertisingRouter advertising router
     * @param age               age
     * @param seqNo             sequence number
     * @param checkSum          checksum
     * @return formatted string
     */
    private StringBuffer getBuffList(String areaId, String routerId, String linkStateId,
                                     String advertisingRouter, int age, String seqNo, String checkSum) {
        StringBuffer strBuf = new StringBuffer();
        strBuf.append(areaId);
        strBuf.append(",");
        strBuf.append(routerId);
        strBuf.append(",");
        strBuf.append(linkStateId);
        strBuf.append(",");
        strBuf.append(advertisingRouter);
        strBuf.append(",");
        strBuf.append(age);
        strBuf.append(",");
        strBuf.append(seqNo);
        strBuf.append(",");
        strBuf.append(checkSum);
        return strBuf;
    }
}