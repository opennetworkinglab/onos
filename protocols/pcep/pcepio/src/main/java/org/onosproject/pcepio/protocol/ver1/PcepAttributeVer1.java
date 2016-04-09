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

package org.onosproject.pcepio.protocol.ver1;

import java.util.LinkedList;
import java.util.ListIterator;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepAttribute;
import org.onosproject.pcepio.protocol.PcepBandwidthObject;
import org.onosproject.pcepio.protocol.PcepIroObject;
import org.onosproject.pcepio.protocol.PcepLspaObject;
import org.onosproject.pcepio.protocol.PcepMetricObject;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP Attribute List.
 */
public class PcepAttributeVer1 implements PcepAttribute {

    /* Reference : RFC5440
     *  where:
     *      <attribute-list>                  ::=[<LSPA>]
     *                                           [<BANDWIDTH>]
     *                                           [<metric-list>]
     *                                           [<IRO>]
     *
     *      <metric-list>                     ::=<METRIC>[<metric-list>]
     */
    protected static final Logger log = LoggerFactory.getLogger(PcepAttributeVer1.class);

    public static final int OBJECT_HEADER_LENGTH = 4;

    //PCEP LSPA Object
    private PcepLspaObject lspaObject;
    private boolean isLspaObjectSet;

    //PCEP Bandwidth Object
    private PcepBandwidthObject bandwidthObject;
    private boolean isBandwidthObjectSet;

    //PCEP Metric list
    private LinkedList<PcepMetricObject> llMetricList;
    private boolean isMetricListSet;

    //PCEP IRO object
    private PcepIroObject iroObject;
    private boolean isIroObjectSet;

    /**
     * Default constructor to initialize member variables.
     */
    public PcepAttributeVer1() {

        lspaObject = null;
        bandwidthObject = null;
        llMetricList = null;
        iroObject = null;
        this.isLspaObjectSet = false;
        this.isBandwidthObjectSet = false;
        this.isMetricListSet = false;
        this.isIroObjectSet = false;
    }

    /**
     * Constructor to initialize all parameters for PCEP attribute.
     *
     * @param lspaObject         PCEP lspa Object.
     * @param bandwidthObject    PCEP bandwidth object.
     * @param llMetricList       list of PCEP metric objects.
     * @param iroObject          PCEP iro object.
     */
    public PcepAttributeVer1(PcepLspaObject lspaObject, PcepBandwidthObject bandwidthObject,
            LinkedList<PcepMetricObject> llMetricList, PcepIroObject iroObject) {

        this.lspaObject = lspaObject;
        this.bandwidthObject = bandwidthObject;
        this.llMetricList = llMetricList;
        this.iroObject = iroObject;
        if (lspaObject == null) {
            this.isLspaObjectSet = false;
        } else {
            this.isLspaObjectSet = true;
        }
        if (bandwidthObject == null) {
            this.isBandwidthObjectSet = false;
        } else {
            this.isBandwidthObjectSet = true;
        }
        if (llMetricList == null) {
            this.isMetricListSet = false;
        } else {
            this.isMetricListSet = true;
        }
        if (iroObject == null) {
            this.isIroObjectSet = false;
        } else {
            this.isIroObjectSet = true;
        }
    }

    /**
     * constructor to initialize bandwidthObject.
     *
     * @param bandwidthObject bandwidth object
     */
    public PcepAttributeVer1(PcepBandwidthObject bandwidthObject) {
        this.isLspaObjectSet = false;

        this.bandwidthObject = bandwidthObject;
        this.isBandwidthObjectSet = true;

        this.isMetricListSet = false;

        this.isIroObjectSet = false;
    }

    /**
     * Parse list for MeticObject.
     *
     * @param cb of type channel buffer
     * @return true if parsing metric list is success
     * @throws PcepParseException when a non metric object is received
     */
    public boolean parseMetricList(ChannelBuffer cb) throws PcepParseException {

        if (llMetricList == null) {
            llMetricList = new LinkedList<>();
        }

        PcepMetricObject metriclist;

        //caller should verify for metric object
        byte yObjClass = PcepMetricObjectVer1.METRIC_OBJ_CLASS;
        byte yObjType = PcepMetricObjectVer1.METRIC_OBJ_TYPE;

        while ((yObjClass == PcepMetricObjectVer1.METRIC_OBJ_CLASS)
                && (yObjType == PcepMetricObjectVer1.METRIC_OBJ_TYPE)) {

            metriclist = PcepMetricObjectVer1.read(cb);
            llMetricList.add(metriclist);
            yObjClass = 0;
            yObjType = 0;

            if (cb.readableBytes() > OBJECT_HEADER_LENGTH) {
                cb.markReaderIndex();
                PcepObjectHeader tempObjHeader = PcepObjectHeader.read(cb);
                cb.resetReaderIndex();
                yObjClass = tempObjHeader.getObjClass();
                yObjType = tempObjHeader.getObjType();
            }
        }
        return true;
    }

    /**
     * Reads lspa , bandwidth , Metriclist and Iro objects and sets the objects.
     *
     * @param cb of type channel buffer
     * @return instance of Pcep Attribute
     * @throws PcepParseException while parsing Pcep Attributes from channel buffer
     */

    public static PcepAttribute read(ChannelBuffer cb) throws PcepParseException {
        if (cb.readableBytes() < OBJECT_HEADER_LENGTH) {
            return null;
        }
        //check whether any pcep attribute is present
        cb.markReaderIndex();
        PcepObjectHeader tempObjHeader = PcepObjectHeader.read(cb);
        cb.resetReaderIndex();
        byte yObjClass = tempObjHeader.getObjClass();

        if (PcepLspaObjectVer1.LSPA_OBJ_CLASS != yObjClass && PcepBandwidthObjectVer1.BANDWIDTH_OBJ_CLASS != yObjClass
                && PcepMetricObjectVer1.METRIC_OBJ_CLASS != yObjClass && PcepIroObjectVer1.IRO_OBJ_CLASS != yObjClass) {
            //No PCEP attribute is present
            return null;
        }

        PcepAttributeVer1 pcepAttribute = new PcepAttributeVer1();

        //If LSPA present then store it.LSPA is optional
        if (yObjClass == PcepLspaObjectVer1.LSPA_OBJ_CLASS) {
            pcepAttribute.setLspaObject(PcepLspaObjectVer1.read(cb));
            yObjClass = checkNextObject(cb);
        }

        //If BANDWIDTH present then store it.BANDWIDTH is optional
        if (yObjClass == PcepBandwidthObjectVer1.BANDWIDTH_OBJ_CLASS) {
            pcepAttribute.setBandwidthObject(PcepBandwidthObjectVer1.read(cb));
            yObjClass = checkNextObject(cb);
        }

        //If Metric list present then store it.MetricList is optional
        if (yObjClass == PcepMetricObjectVer1.METRIC_OBJ_CLASS) {
            pcepAttribute.parseMetricList(cb);
            yObjClass = checkNextObject(cb);
        }

        //If IRO present then store it.IRO is optional
        if (yObjClass == PcepIroObjectVer1.IRO_OBJ_CLASS) {
            pcepAttribute.setIroObject(PcepIroObjectVer1.read(cb));
        }

        PcepLspaObject lspaObject = pcepAttribute.getLspaObject();
        PcepBandwidthObject bandwidthObject = pcepAttribute.getBandwidthObject();
        LinkedList<PcepMetricObject> metriclist = pcepAttribute.llMetricList;
        PcepIroObject iroObject = pcepAttribute.getIroObject();

        return new PcepAttributeVer1(lspaObject, bandwidthObject, metriclist, iroObject);
    }

    /**
     * Checks whether there is a more object or not.
     *
     * @param cb of type channel buffer
     * @return instance of object header
     */
    private static byte checkNextObject(ChannelBuffer cb) {
        if (cb.readableBytes() < OBJECT_HEADER_LENGTH) {
            return 0;
        }
        cb.markReaderIndex();
        PcepObjectHeader tempObjHeader = PcepObjectHeader.read(cb);
        cb.resetReaderIndex();
        return tempObjHeader.getObjClass();
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {
        int iLenStartIndex = cb.writerIndex();
        //PCEP LSPA object is optional
        if (this.isLspaObjectSet) {
            this.lspaObject.write(cb);
        }

        //PCEP BANDWIDTH object is optional
        if (this.isBandwidthObjectSet) {
            this.bandwidthObject.write(cb);
        }

        //PCEP Metric list is optional
        if (this.isMetricListSet) {
            ListIterator<PcepMetricObject> listIterator = this.llMetricList.listIterator();
            while (listIterator.hasNext()) {
                listIterator.next().write(cb);
            }
        }

        //PCEP  IRO object is optional
        if (this.isIroObjectSet) {
            this.iroObject.write(cb);
        }
        return cb.writerIndex() - iLenStartIndex;
    }

    @Override
    public PcepLspaObject getLspaObject() {
        return lspaObject;
    }

    @Override
    public PcepBandwidthObject getBandwidthObject() {
        return bandwidthObject;
    }

    @Override
    public LinkedList<PcepMetricObject> getMetricObjectList() {
        return llMetricList;
    }

    @Override
    public PcepIroObject getIroObject() {
        return iroObject;
    }

    @Override
    public void setBandwidthObject(PcepBandwidthObject bandwidthObject) {
        this.isBandwidthObjectSet = true;
        this.bandwidthObject = bandwidthObject;
    }

    @Override
    public void setMetricObjectList(LinkedList<PcepMetricObject> llMetricList) {
        this.isMetricListSet = true;
        this.llMetricList = llMetricList;

    }

    @Override
    public void setLspaObject(PcepLspaObject lspaObject) {
        this.isLspaObjectSet = true;
        this.lspaObject = lspaObject;
    }

    @Override
    public void setIroObject(PcepIroObject iroObject) {
        this.isIroObjectSet = true;
        this.iroObject = iroObject;
    }

    /**
     * Builder class for PCEP attributes.
     */
    public static class Builder implements PcepAttribute.Builder {

        //PCEP LSPA Object
        private PcepLspaObject lspaObject;
        private boolean isLspaObjectSet;

        //PCEP BANDWIDTH Object
        private PcepBandwidthObject bandwidthObject;
        private boolean isBandwidthObjectSet;

        //PCEP Metric list
        private LinkedList<PcepMetricObject> llMetricList;
        private boolean isMetricListSet;

        //PCEP IRO object
        private PcepIroObject iroObject;
        private boolean isIroObjectSet;

        @Override
        public PcepAttribute build() {

            //PCEP LSPA Object
            PcepLspaObject lspaObject = null;

            //PCEP BANDWIDTH Object
            PcepBandwidthObject bandwidthObject = null;

            //PCEP Metric list
            LinkedList<PcepMetricObject> llMetricList = null;

            //PCEP IRO object
            PcepIroObject iroObject = null;

            if (this.isLspaObjectSet) {
                lspaObject = this.lspaObject;
            }
            if (this.isBandwidthObjectSet) {
                bandwidthObject = this.bandwidthObject;
            }
            if (this.isMetricListSet) {
                llMetricList = this.llMetricList;
            }
            if (this.isIroObjectSet) {
                iroObject = this.iroObject;
            }
            return new PcepAttributeVer1(lspaObject, bandwidthObject, llMetricList, iroObject);
        }

        @Override
        public PcepLspaObject getLspaObject() {
            return this.lspaObject;
        }

        @Override
        public PcepBandwidthObject getBandwidthObject() {
            return this.bandwidthObject;
        }

        @Override
        public LinkedList<PcepMetricObject> getMetricObjectList() {
            return this.llMetricList;
        }

        @Override
        public PcepIroObject getIroObject() {
            return this.iroObject;
        }

        @Override
        public Builder setBandwidthObject(PcepBandwidthObject bandwidthObject) {
            this.isBandwidthObjectSet = true;
            this.bandwidthObject = bandwidthObject;
            return this;
        }

        @Override
        public Builder setMetricObjectList(LinkedList<PcepMetricObject> llMetricList) {
            this.isMetricListSet = true;
            this.llMetricList = llMetricList;
            return this;
        }

        @Override
        public Builder setLspaObject(PcepLspaObject lspaObject) {
            this.isLspaObjectSet = true;
            this.lspaObject = lspaObject;
            return this;
        }

        @Override
        public Builder setIroObject(PcepIroObject iroObject) {
            this.isIroObjectSet = true;
            this.iroObject = iroObject;
            return this;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("lspaObject", lspaObject)
                .add("bandwidthObject", bandwidthObject)
                .add("MetricObjectList", llMetricList)
                .add("IroObject", iroObject)
                .toString();
    }
}
