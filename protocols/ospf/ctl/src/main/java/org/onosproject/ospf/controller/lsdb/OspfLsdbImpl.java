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
package org.onosproject.ospf.controller.lsdb;

import com.google.common.base.Objects;
import org.onosproject.ospf.controller.LsaBin;
import org.onosproject.ospf.controller.LsaWrapper;
import org.onosproject.ospf.controller.LsdbAge;
import org.onosproject.ospf.controller.OspfArea;
import org.onosproject.ospf.controller.OspfInterface;
import org.onosproject.ospf.controller.OspfLsaType;
import org.onosproject.ospf.controller.OspfLsdb;
import org.onosproject.ospf.controller.area.OspfAreaImpl;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.OpaqueLsaHeader;
import org.onosproject.ospf.protocol.util.OspfParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents the Link State Database.
 */
public class OspfLsdbImpl implements OspfLsdb {
    private static final Logger log = LoggerFactory.getLogger(OspfLsdbImpl.class);
    private Map routerLsas = new HashMap();
    private Map networkLsas = new HashMap();
    private Map summaryLsas = new HashMap();
    private Map asbrSummaryLSAs = new HashMap();
    private Map opaque9Lsas = new HashMap();
    private Map opaque10Lsas = new HashMap();
    private Map opaque11Lsas = new HashMap();
    private Map externalLsas = new HashMap();
    private long routerLsaSeqNo = OspfParameters.STARTLSSEQUENCENUM;
    private long networkLsaSeqNo = OspfParameters.STARTLSSEQUENCENUM;
    private LsdbAge lsdbAge = null;
    private OspfArea ospfArea = null;


    /**
     * Creates an instance of OSPF LSDB.
     *
     * @param ospfArea area instance
     */
    public OspfLsdbImpl(OspfArea ospfArea) {
        this.ospfArea = ospfArea;
        lsdbAge = new LsdbAgeImpl(ospfArea);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OspfLsdbImpl that = (OspfLsdbImpl) o;
        return Objects.equal(routerLsas.size(), that.routerLsas.size()) &&
                Objects.equal(networkLsas.size(), that.networkLsas.size()) &&
                Objects.equal(summaryLsas.size(), that.summaryLsas.size()) &&
                Objects.equal(asbrSummaryLSAs.size(), that.asbrSummaryLSAs.size()) &&
                Objects.equal(lsdbAge, that.lsdbAge) &&
                Objects.equal(routerLsaSeqNo, that.routerLsaSeqNo) &&
                Objects.equal(networkLsaSeqNo, that.networkLsaSeqNo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(routerLsas, networkLsas, summaryLsas, asbrSummaryLSAs, lsdbAge,
                                routerLsaSeqNo, networkLsaSeqNo);
    }

    /**
     * Initializes the link state database.
     */
    public void initializeDb() {
        lsdbAge.startDbAging();
    }

    /**
     * Returns all LSA Headers (Router and Summary) in a Vector.
     *
     * @param excludeMaxAgeLsa exclude the max age LSAs
     * @param isOpaqueCapable  is opaque capable or not
     * @return List of LSA headers
     */
    public List getAllLsaHeaders(boolean excludeMaxAgeLsa, boolean isOpaqueCapable) {
        List summList = new CopyOnWriteArrayList();
        addLsaToHeaderList(summList, excludeMaxAgeLsa, routerLsas);
        addLsaToHeaderList(summList, excludeMaxAgeLsa, networkLsas);
        addLsaToHeaderList(summList, excludeMaxAgeLsa, summaryLsas);
        addLsaToHeaderList(summList, excludeMaxAgeLsa, asbrSummaryLSAs);
        addLsaToHeaderList(summList, excludeMaxAgeLsa, externalLsas);
        if (isOpaqueCapable) {
            addLsaToHeaderList(summList, excludeMaxAgeLsa, opaque9Lsas);
            addLsaToHeaderList(summList, excludeMaxAgeLsa, opaque10Lsas);
            addLsaToHeaderList(summList, excludeMaxAgeLsa, opaque11Lsas);
        }

        return summList;
    }

    /**
     * Adds the LSAs to summary list.
     *
     * @param summList         summary list
     * @param excludeMaxAgeLsa exclude max age LSA
     * @param lsaMap           map of LSA
     */
    private void addLsaToHeaderList(List summList, boolean excludeMaxAgeLsa, Map lsaMap) {
        Iterator slotVals = lsaMap.values().iterator();
        while (slotVals.hasNext()) {
            LsaWrapper wrapper = (LsaWrapper) slotVals.next();
            if (excludeMaxAgeLsa) {
                //if current age of lsa is max age or lsa present in Max Age bin
                if (wrapper.currentAge() != OspfParameters.MAXAGE &&
                        lsdbAge.getMaxAgeBin().ospfLsa(((OspfAreaImpl)
                                ospfArea).getLsaKey(((LsaWrapperImpl) wrapper).lsaHeader())) == null) {
                    addToList(wrapper, summList);
                }
            } else {
                addToList(wrapper, summList);
            }
        }
    }

    /**
     * Adds the LSWrapper to summary list.
     *
     * @param wrapper  LSA wrapper instance
     * @param summList LSA summary list
     */
    private void addToList(LsaWrapper wrapper, List summList) {
        LsaHeader header = (LsaHeader) wrapper.ospfLsa();
        //set the current age
        header.setAge(wrapper.currentAge());
        summList.add(header);
    }

    /**
     * Gets the LSDB LSA key from Lsa Header.
     *
     * @param lsaHeader LSA header instance
     * @return key
     */
    public String getLsaKey(LsaHeader lsaHeader) {
        String lsaKey = "";
        switch (lsaHeader.lsType()) {
            case OspfParameters.LINK_LOCAL_OPAQUE_LSA:
            case OspfParameters.AREA_LOCAL_OPAQUE_LSA:
            case OspfParameters.AS_OPAQUE_LSA:
                OpaqueLsaHeader header = (OpaqueLsaHeader) lsaHeader;
                lsaKey = lsaHeader.lsType() + "-" + header.opaqueType() + header.opaqueId() + "-" +
                        lsaHeader.advertisingRouter();
                break;
            case OspfParameters.ROUTER:
            case OspfParameters.NETWORK:
            case OspfParameters.ASBR_SUMMARY:
            case OspfParameters.SUMMARY:
            case OspfParameters.EXTERNAL_LSA:
                lsaKey = lsaHeader.lsType() + "-" + lsaHeader.linkStateId() + "-" +
                        lsaHeader.advertisingRouter();
                break;
            default:
                log.debug("Unknown LSA type..!!!");
                break;
        }

        return lsaKey;
    }

    /**
     * Gets wrapper instance in LSDB.
     *
     * @param lsaHeader LSA header instance.
     * @return LSA Wrapper instance.
     */
    public LsaWrapper lsaLookup(LsaHeader lsaHeader) {

        return findLsa(lsaHeader.lsType(), getLsaKey(lsaHeader));
    }

    /**
     * Finds the LSA from appropriate maps.
     *
     * @param lsType type of LSA
     * @param lsaKey key
     * @return LSA wrapper object
     */
    public LsaWrapper findLsa(int lsType, String lsaKey) {
        LsaWrapper lsaWrapper = null;

        switch (lsType) {
            case OspfParameters.LINK_LOCAL_OPAQUE_LSA:
                lsaWrapper = (LsaWrapper) opaque9Lsas.get(lsaKey);
                break;
            case OspfParameters.AREA_LOCAL_OPAQUE_LSA:
                lsaWrapper = (LsaWrapper) opaque10Lsas.get(lsaKey);
                break;
            case OspfParameters.AS_OPAQUE_LSA:
                lsaWrapper = (LsaWrapper) opaque11Lsas.get(lsaKey);
                break;
            case OspfParameters.ROUTER:
                lsaWrapper = (LsaWrapper) routerLsas.get(lsaKey);
                break;
            case OspfParameters.NETWORK:
                lsaWrapper = (LsaWrapper) networkLsas.get(lsaKey);
                break;
            case OspfParameters.ASBR_SUMMARY:
                lsaWrapper = (LsaWrapper) asbrSummaryLSAs.get(lsaKey);
                break;
            case OspfParameters.SUMMARY:
                lsaWrapper = (LsaWrapper) summaryLsas.get(lsaKey);
                break;
            case OspfParameters.EXTERNAL_LSA:
                lsaWrapper = (LsaWrapper) externalLsas.get(lsaKey);
                break;
            default:
                log.debug("Unknown LSA type..!!!");
                break;
        }

        //set the current age
        if (lsaWrapper != null) {
            //set the current age
            ((LsaWrapperImpl) lsaWrapper).lsaHeader().setAge(lsaWrapper.currentAge());
            ((LsaHeader) lsaWrapper.ospfLsa()).setAge(lsaWrapper.currentAge());
        }

        return lsaWrapper;
    }


    /**
     * Installs a new self-originated LSA if possible.
     * Return true if installing was successful else false.
     *
     * @param newLsa           LSA header instance
     * @param isSelfOriginated is self originated or not
     * @param ospfInterface    OSPF interface instance
     * @return true if successfully added
     */
    public boolean addLsa(LsaHeader newLsa, boolean isSelfOriginated, OspfInterface ospfInterface) {

        LsaWrapperImpl lsaWrapper = new LsaWrapperImpl();
        lsaWrapper.setLsaType(newLsa.getOspfLsaType());
        lsaWrapper.setOspfLsa(newLsa);
        lsaWrapper.setLsaHeader(newLsa);
        lsaWrapper.setLsaAgeReceived(newLsa.age());
        lsaWrapper.setAgeCounterWhenReceived(lsdbAge.getAgeCounter());
        lsaWrapper.setAgeCounterRollOverWhenAdded(lsdbAge.getAgeCounterRollOver());
        lsaWrapper.setIsSelfOriginated(isSelfOriginated);
        lsaWrapper.setIsSelfOriginated(isSelfOriginated);
        lsaWrapper.setOspfInterface(ospfInterface);
        lsaWrapper.setLsdbAge(lsdbAge);
        addLsa(lsaWrapper);

        log.debug("Added LSA In LSDB: {}", newLsa);

        return true;
    }

    /**
     * Installs a new self-originated LSA if possible.
     * Return true if installing was successful else false.
     * Adding LSA In cases
     * 1) New Self Originated LSA based on change in topology
     * 2) New Self Originated LSA because of LSRefresh
     * 2) New LSA received via Link State Update Packet
     *
     * @param newLsa LSA wrapper instance
     * @return true if added successfully
     */
    private boolean addLsa(LsaWrapper newLsa) {
        // adding an LSA - verify if it's old or new
        // verify min failed
        // to verify if it's a new LSA or updating the old LSA .
        // fetch the LSA Type
        // verify if the LSA age is ! Max Age
        // a) it is  received during the flooding process (Section 13)
        // b) it is originated by the router itself (Section 12.4)
        // start aging .
        String key = getLsaKey(((LsaWrapperImpl) newLsa).lsaHeader());
        //Remove the lsa from bin if exist. we will be adding it in new bin based on the current age.
        removeLsaFromBin(lsaLookup(((LsaWrapperImpl) newLsa).lsaHeader()));

        switch (((LsaWrapperImpl) newLsa).lsaHeader().lsType()) {

            case OspfParameters.LINK_LOCAL_OPAQUE_LSA:
                opaque9Lsas.put(key, newLsa);
                break;
            case OspfParameters.AREA_LOCAL_OPAQUE_LSA:
                opaque10Lsas.put(key, newLsa);
                break;
            case OspfParameters.AS_OPAQUE_LSA:
                opaque11Lsas.put(key, newLsa);
                break;
            case OspfParameters.ROUTER:
                routerLsas.put(key, newLsa);
                break;
            case OspfParameters.NETWORK:
                networkLsas.put(key, newLsa);
                break;
            case OspfParameters.ASBR_SUMMARY:
                asbrSummaryLSAs.put(key, newLsa);
                break;
            case OspfParameters.SUMMARY:
                summaryLsas.put(key, newLsa);
                break;
            case OspfParameters.EXTERNAL_LSA:
                externalLsas.put(key, newLsa);
                break;
            default:
                log.debug("Unknown LSA type to add..!!!");
                break;
        }
        //add it to bin
        Integer binNumber = lsdbAge.age2Bin(((LsaWrapperImpl) newLsa).lsaHeader().age());
        LsaBin lsaBin = lsdbAge.getLsaBin(binNumber);
        if (lsaBin != null) {
            //remove from existing
            newLsa.setBinNumber(binNumber);
            lsaBin.addOspfLsa(key, newLsa);
            lsdbAge.addLsaBin(binNumber, lsaBin);
            log.debug("Added Type {} LSA to LSDB and LSABin[{}], Age of LSA {}", newLsa.lsaType(),
                      binNumber, ((LsaWrapperImpl) newLsa).lsaHeader().age());
        }

        return false;
    }

    /**
     * Adds the LSA to maxAge bin.
     *
     * @param key     key
     * @param wrapper LSA wrapper instance
     */
    public void addLsaToMaxAgeBin(String key, Object wrapper) {
        lsdbAge.addLsaToMaxAgeBin(key, (LsaWrapper) wrapper);
    }

    /**
     * Removes LSA from Bin.
     *
     * @param lsaWrapper LSA wrapper instance
     */
    public void removeLsaFromBin(Object lsaWrapper) {
        if (lsaWrapper != null) {
            lsdbAge.removeLsaFromBin((LsaWrapper) lsaWrapper);
        }
    }

    /**
     * RFC 2328 - Section 13.1.  Determining which LSA is newer.
     *
     * @param lsa1 LSA instance
     * @param lsa2 LSA instance
     * @return string status
     */
    public String isNewerOrSameLsa(LsaHeader lsa1, LsaHeader lsa2) {
        if (lsa1.lsSequenceNo() > lsa2.lsSequenceNo()) {
            return "latest";
        } else if (lsa1.lsSequenceNo() < lsa2.lsSequenceNo()) {
            return "old";
        } else if (lsa1.lsSequenceNo() == lsa2.lsSequenceNo()) {
            if (lsa1.lsCheckSum() > lsa2.lsCheckSum()) {
                return "latest";
            } else if (lsa1.lsCheckSum() < lsa2.lsCheckSum()) {
                return "old";
            } else if (lsa1.lsCheckSum() == lsa2.lsCheckSum()) {
                if (lsa1.age() == lsa2.age()) {
                    return "same";
                } else if (lsa1.age() == OspfParameters.MAXAGE) {
                    return "latest";
                } else if (lsa2.age() == OspfParameters.MAXAGE) {
                    return "old";
                } else if (OspfParameters.MAXAGEDIFF == (lsa1.age() - lsa2.age())) {
                    if (lsa1.age() < lsa2.age()) {
                        return "latest";
                    } else {
                        return "old";
                    }
                } else {
                    return "same";
                }
            }
        }

        return "";
    }

    /**
     * Gets the sequence number.
     *
     * @param lsaType type of LSA
     * @return sequence number
     */
    public long getLsSequenceNumber(OspfLsaType lsaType) {
        switch (lsaType) {
            case ROUTER:
                return routerLsaSeqNo++;
            case NETWORK:
                return networkLsaSeqNo++;
            default:
                return OspfParameters.STARTLSSEQUENCENUM;
        }
    }

    /**
     * Deletes the given LSA.
     *
     * @param lsaHeader LSA header instance
     */
    public void deleteLsa(LsaHeader lsaHeader) {

        String lsaKey = getLsaKey(lsaHeader);
        switch (lsaHeader.lsType()) {
            case OspfParameters.LINK_LOCAL_OPAQUE_LSA:
                opaque9Lsas.remove(lsaKey);
                break;
            case OspfParameters.AREA_LOCAL_OPAQUE_LSA:
                opaque10Lsas.remove(lsaKey);
                break;
            case OspfParameters.AS_OPAQUE_LSA:
                opaque11Lsas.remove(lsaKey);
                break;
            case OspfParameters.ROUTER:
                routerLsas.remove(lsaKey);
                break;
            case OspfParameters.NETWORK:
                networkLsas.remove(lsaKey);
                break;
            case OspfParameters.ASBR_SUMMARY:
                asbrSummaryLSAs.remove(lsaKey);
                break;
            case OspfParameters.SUMMARY:
                summaryLsas.remove(lsaKey);
                break;
            case OspfParameters.EXTERNAL_LSA:
                externalLsas.remove(lsaKey);
                break;
            default:
                log.debug("Unknown LSA type to delete..!!!");
                break;
        }
    }

    /**
     * Sets sequence number.
     *
     * @param routerLsaSeqNo sequence number
     */
    public void setRouterLsaSeqNo(long routerLsaSeqNo) {
        this.routerLsaSeqNo = routerLsaSeqNo;
    }

    /**
     * Sets sequence number.
     *
     * @param networkLsaSeqNo sequence number
     */
    public void setNetworkLsaSeqNo(long networkLsaSeqNo) {
        this.networkLsaSeqNo = networkLsaSeqNo;
    }
}