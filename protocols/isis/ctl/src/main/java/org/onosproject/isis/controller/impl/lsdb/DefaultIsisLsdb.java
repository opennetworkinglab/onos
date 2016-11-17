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
package org.onosproject.isis.controller.impl.lsdb;

import org.jboss.netty.buffer.ChannelBuffers;
import org.onosproject.isis.controller.IsisInterface;
import org.onosproject.isis.controller.IsisLsdb;
import org.onosproject.isis.controller.IsisLsdbAge;
import org.onosproject.isis.controller.IsisLspBin;
import org.onosproject.isis.controller.IsisMessage;
import org.onosproject.isis.controller.IsisNeighbor;
import org.onosproject.isis.controller.IsisPduType;
import org.onosproject.isis.controller.IsisRouterType;
import org.onosproject.isis.controller.LspWrapper;
import org.onosproject.isis.controller.impl.Controller;
import org.onosproject.isis.controller.impl.LspEventConsumer;
import org.onosproject.isis.io.isispacket.pdu.LsPdu;
import org.onosproject.isis.io.util.IsisConstants;
import org.onosproject.isis.io.util.IsisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Representation of ISIS link state database.
 */
public class DefaultIsisLsdb implements IsisLsdb {
    private static final Logger log = LoggerFactory.getLogger(DefaultIsisLsdb.class);
    private Map<String, LspWrapper> isisL1Db = new ConcurrentHashMap<>();
    private Map<String, LspWrapper> isisL2Db = new ConcurrentHashMap<>();
    private IsisLsdbAge lsdbAge = null;
    private Controller controller = null;
    private List<IsisInterface> isisInterfaceList = new ArrayList<>();


    private int l1LspSeqNo = IsisConstants.STARTLSSEQUENCENUM;
    private int l2LspSeqNo = IsisConstants.STARTLSSEQUENCENUM;
    private LspEventConsumer queueConsumer = null;
    private BlockingQueue<LspWrapper> lspForProviderQueue = new ArrayBlockingQueue<>(1024);

    /**
     * Creates an instance of ISIS LSDB.
     */
    public DefaultIsisLsdb() {
        lsdbAge = new DefaultIsisLsdbAge();
    }

    /**
     * Sets the controller instance.
     *
     * @param controller controller instance
     */
    public void setController(Controller controller) {
        this.controller = controller;
    }

    /**
     * Sets the list of IsisInterface instance.
     *
     * @param isisInterfaceList isisInterface instance
     */
    public void setIsisInterface(List<IsisInterface> isisInterfaceList) {
        this.isisInterfaceList = isisInterfaceList;
    }

    /**
     * Initializes the link state database.
     */
    public void initializeDb() {
        lsdbAge.startDbAging();
        queueConsumer = new LspEventConsumer(lspForProviderQueue, controller);
        new Thread(queueConsumer).start();
    }

    /**
     * Sets the level 1 link state sequence number.
     *
     * @param l1LspSeqNo link state sequence number
     */
    public void setL1LspSeqNo(int l1LspSeqNo) {
        this.l1LspSeqNo = l1LspSeqNo;
    }

    /**
     * Sets the level 2 link state sequence number.
     *
     * @param l2LspSeqNo link state sequence number
     */
    public void setL2LspSeqNo(int l2LspSeqNo) {
        this.l2LspSeqNo = l2LspSeqNo;
    }

    /**
     * Returns the LSDB LSP key.
     *
     * @param systemId system ID
     * @return key
     */
    public String lspKey(String systemId) {
        StringBuilder lspKey = new StringBuilder();
        lspKey.append(systemId);
        lspKey.append(".00");
        lspKey.append("-");
        lspKey.append("00");

        return lspKey.toString();
    }

    /**
     * Returns the neighbor L1 database information.
     *
     * @return neighbor L1 database information
     */
    public Map<String, LspWrapper> getL1Db() {
        return isisL1Db;
    }

    /**
     * Returns the neighbor L2 database information.
     *
     * @return neighbor L2 database information
     */
    public Map<String, LspWrapper> getL2Db() {
        return isisL2Db;
    }

    /**
     * Returns the LSDB instance.
     *
     * @return LSDB instance
     */
    public IsisLsdb isisLsdb() {
        return this;
    }

    /**
     * Returns all LSPs (L1 and L2).
     *
     * @param excludeMaxAgeLsp exclude the max age LSPs
     * @return List of LSPs
     */
    public List<LspWrapper> allLspHeaders(boolean excludeMaxAgeLsp) {
        List<LspWrapper> summaryList = new CopyOnWriteArrayList<>();
        addLspToHeaderList(summaryList, excludeMaxAgeLsp, isisL1Db);
        addLspToHeaderList(summaryList, excludeMaxAgeLsp, isisL2Db);

        return summaryList;
    }

    /**
     * Adds the LSPs to summary list.
     *
     * @param summaryList      summary list
     * @param excludeMaxAgeLsp exclude max age LSP
     * @param lspMap           map of LSP
     */
    private void addLspToHeaderList(List summaryList, boolean excludeMaxAgeLsp, Map lspMap) {
        Iterator slotVals = lspMap.values().iterator();
        while (slotVals.hasNext()) {
            LspWrapper wrapper = (LspWrapper) slotVals.next();
            if (excludeMaxAgeLsp) {
                //if current age of lsa is max age or lsa present in Max Age bin
                if (wrapper.remainingLifetime() != 0) {
                    addToList(wrapper, summaryList);
                }
            } else {
                addToList(wrapper, summaryList);
            }
        }
    }

    /**
     * Adds the LSPWrapper to summary list.
     *
     * @param wrapper  LSP wrapper instance
     * @param summList LSP summary list
     */
    private void addToList(LspWrapper wrapper, List summList) {
        //set the current age
        ((LsPdu) wrapper.lsPdu()).setRemainingLifeTime(wrapper.remainingLifetime());
        summList.add(wrapper);
    }

    /**
     * Finds the LSP from appropriate maps L1 or L2 based on type.
     *
     * @param pduType L1 or L2 LSP
     * @param lspId   LSP ID
     * @return LSP wrapper object
     */
    public LspWrapper findLsp(IsisPduType pduType, String lspId) {
        LspWrapper lspWrapper = null;

        switch (pduType) {
            case L1LSPDU:
                lspWrapper = isisL1Db.get(lspId);
                break;
            case L2LSPDU:
                lspWrapper = isisL2Db.get(lspId);
                break;
            default:
                log.debug("Unknown LSP type..!!!");
                break;
        }

        //set the current age
        if (lspWrapper != null) {
            //set the current age
            ((DefaultLspWrapper) lspWrapper).lsPdu().setRemainingLifeTime(lspWrapper.remainingLifetime());
        }

        return lspWrapper;
    }

    /**
     * Installs a new self-originated LSP.
     *
     * @param isisMessage ISIS message
     * @param isSelfOriginated is the message self originated?
     * @param isisInterface ISIS interface
     * @return true if successfully added
     */
    public boolean addLsp(IsisMessage isisMessage, boolean isSelfOriginated, IsisInterface isisInterface) {
        LsPdu lspdu = (LsPdu) isisMessage;
        if (isSelfOriginated) {
            //Add length and checksum
            byte[] lspBytes = lspdu.asBytes();
            lspdu.setPduLength(lspBytes.length);
            lspBytes = IsisUtil.addChecksum(lspBytes, IsisConstants.CHECKSUMPOSITION,
                    IsisConstants.CHECKSUMPOSITION + 1);
            byte[] checkSum = {lspBytes[IsisConstants.CHECKSUMPOSITION], lspBytes[IsisConstants.CHECKSUMPOSITION + 1]};
            lspdu.setCheckSum(ChannelBuffers.copiedBuffer(checkSum).readUnsignedShort());
        }

        DefaultLspWrapper lspWrapper = (DefaultLspWrapper) findLsp(lspdu.isisPduType(), lspdu.lspId());
        if (lspWrapper == null) {
            lspWrapper = new DefaultLspWrapper();
        }

        lspWrapper.setLspAgeReceived(IsisConstants.LSPMAXAGE - lspdu.remainingLifeTime());
        lspWrapper.setLspType(IsisPduType.get(lspdu.pduType()));
        lspWrapper.setLsPdu(lspdu);
        lspWrapper.setAgeCounterWhenReceived(lsdbAge.ageCounter());
        lspWrapper.setAgeCounterRollOverWhenAdded(lsdbAge.ageCounterRollOver());
        lspWrapper.setSelfOriginated(isSelfOriginated);
        lspWrapper.setIsisInterface(isisInterface);
        lspWrapper.setLsdbAge(lsdbAge);
        addLsp(lspWrapper, lspdu.lspId());

        log.debug("Added LSp In LSDB: {}", lspWrapper);
        try {
            if (!lspWrapper.isSelfOriginated()) {
                lspWrapper.setLspProcessing(IsisConstants.LSPADDED);
                lspForProviderQueue.put(lspWrapper);
            }
        } catch (Exception e) {
            log.debug("Added LSp In Blocking queue: {}", lspWrapper);
        }
        return true;
    }

    /**
     * Adds the LSP to L1 or L2 database.
     *
     * @param lspWrapper LSA wrapper instance
     * @param key        key
     * @return True if added else false
     */
    private boolean addLsp(LspWrapper lspWrapper, String key) {
        //Remove the lsa from bin if exist.
        removeLspFromBin(lspWrapper);

        switch (lspWrapper.lsPdu().isisPduType()) {
            case L1LSPDU:
                isisL1Db.remove(key);
                isisL1Db.put(key, lspWrapper);
                break;
            case L2LSPDU:
                isisL2Db.remove(key);
                isisL2Db.put(key, lspWrapper);
                break;
            default:
                log.debug("Unknown LSP type to add..!!!");
                break;
        }

        //add it to bin
        Integer binNumber = lsdbAge.age2Bin(IsisConstants.LSPMAXAGE - lspWrapper.lspAgeReceived());
        IsisLspBin lspBin = lsdbAge.getLspBin(binNumber);
        if (lspBin != null) {
            //remove from existing
            lspWrapper.setBinNumber(binNumber);
            lspBin.addIsisLsp(key, lspWrapper);
            lsdbAge.addLspBin(binNumber, lspBin);
            log.debug("Added Type {} LSP to LSDB and LSABin[{}], Remaining life time of LSA {}",
                    lspWrapper.lsPdu().isisPduType(),
                    binNumber, lspWrapper.remainingLifetime());
        }

        return false;
    }

    /**
     * Removes LSP from Bin.
     *
     * @param lsaWrapper LSP wrapper instance
     */
    public void removeLspFromBin(LspWrapper lsaWrapper) {
        if (lsaWrapper != null) {
            lsdbAge.removeLspFromBin(lsaWrapper);
        }
    }

    /**
     * Returns new ,latest or old according to the type of ISIS message received.
     *
     * @param lsp1 LSP instance
     * @param lsp2 LSP instance
     * @return string status
     */
    public String isNewerOrSameLsp(IsisMessage lsp1, IsisMessage lsp2) {
        LsPdu receivedLsp = (LsPdu) lsp1;
        LsPdu lspFromDb = (LsPdu) lsp2;
        if (receivedLsp.sequenceNumber() > lspFromDb.sequenceNumber() ||
                receivedLsp.checkSum() != lspFromDb.checkSum()) {
            return "latest";
        } else if (receivedLsp.sequenceNumber() < lspFromDb.sequenceNumber()) {
            return "old";
        } else if (receivedLsp.sequenceNumber() == lspFromDb.sequenceNumber()) {
            return "same";
        }

        return "";
    }

    /**
     * Returns the sequence number.
     *
     * @param lspType type of LSP
     * @return sequence number
     */
    public int lsSequenceNumber(IsisPduType lspType) {
        switch (lspType) {
            case L1LSPDU:
                return l1LspSeqNo++;
            case L2LSPDU:
                return l2LspSeqNo++;
            default:
                return IsisConstants.STARTLSSEQUENCENUM;
        }
    }

    /**
     * Deletes the given LSP.
     *
     * @param lspMessage LSP instance
     */
    public void deleteLsp(IsisMessage lspMessage) {
        LsPdu lsp = (LsPdu) lspMessage;
        String lspKey = lsp.lspId();
        LspWrapper lspWrapper = findLsp(lspMessage.isisPduType(), lspKey);
        switch (lsp.isisPduType()) {
            case L1LSPDU:
                isisL1Db.remove(lspKey);
                break;
            case L2LSPDU:
                isisL2Db.remove(lspKey);
                break;
            default:
                log.debug("Unknown LSP type to remove..!!!");
                break;
        }

        try {
            lspWrapper.setLspProcessing(IsisConstants.LSPREMOVED);
            lspForProviderQueue.put(lspWrapper);
        } catch (Exception e) {
            log.debug("Added LSp In Blocking queue: {}", lspWrapper);
        }
    }

    /**
     * Removes topology information when neighbor down.
     *
     * @param neighbor      ISIS neighbor instance
     * @param isisInterface ISIS interface instance
     */
    public void removeTopology(IsisNeighbor neighbor, IsisInterface isisInterface) {
        String lspKey = neighbor.neighborSystemId() + ".00-00";
        LspWrapper lspWrapper = null;
        switch (IsisRouterType.get(isisInterface.reservedPacketCircuitType())) {
            case L1:
                lspWrapper = findLsp(IsisPduType.L1LSPDU, lspKey);
                break;
            case L2:
                lspWrapper = findLsp(IsisPduType.L2LSPDU, lspKey);
                break;
            case L1L2:
                lspWrapper = findLsp(IsisPduType.L1LSPDU, lspKey);
                if (lspWrapper == null) {
                    lspWrapper = findLsp(IsisPduType.L2LSPDU, lspKey);
                }
                break;
            default:
                log.debug("Unknown type");
        }
        try {
            if (lspWrapper != null) {
                lspWrapper.setLspProcessing(IsisConstants.LSPREMOVED);
                lspForProviderQueue.put(lspWrapper);
            }
        } catch (Exception e) {
            log.debug("Added LSp In Blocking queue: {}", lspWrapper);
        }
    }
}
