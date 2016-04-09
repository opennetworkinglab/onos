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
import org.onosproject.pcepio.protocol.PcepLabelRange;
import org.onosproject.pcepio.protocol.PcepLabelRangeObject;
import org.onosproject.pcepio.protocol.PcepSrpObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP Label Range.
 */
public class PcepLabelRangeVer1 implements PcepLabelRange {

    protected static final Logger log = LoggerFactory.getLogger(PcepLabelRangeVer1.class);

    /*
        <label-range> ::= <SRP>
                          <labelrange-list>
        Where
                <labelrange-list>::=<LABEL-RANGE>[<labelrange-list>]
     */

    // PCEP SRP Object
    private PcepSrpObject srpObject;
    //<labelrange-list> of type PcepLabelRangeObject.
    private LinkedList<PcepLabelRangeObject> llLabelRangeList;

    /**
     * Default Constructor.
     */
    public PcepLabelRangeVer1() {
        srpObject = null;
        llLabelRangeList = null;
    }

    /**
     * Constructor to initialize objects.
     *
     * @param srpObj PCEP Srp object.
     * @param llLabelRangeList list of PcepLabelRangeObject.
     */
    PcepLabelRangeVer1(PcepSrpObject srpObj, LinkedList<PcepLabelRangeObject> llLabelRangeList) {
        this.srpObject = srpObj;
        this.llLabelRangeList = llLabelRangeList;
    }

    @Override
    public PcepSrpObject getSrpObject() {
        return srpObject;
    }

    @Override
    public void setSrpObject(PcepSrpObject srpObject) {
        this.srpObject = srpObject;

    }

    @Override
    public LinkedList<PcepLabelRangeObject> getLabelRangeList() {
        return llLabelRangeList;
    }

    @Override
    public void setLabelRangeList(LinkedList<PcepLabelRangeObject> ll) {
        this.llLabelRangeList = ll;
    }

    /**
     * Reads channel buffer and returns object of PcepLabelRange.
     *
     * @param cb of type channel buffer.
     * @return object of PcepLabelRange
     * @throws PcepParseException when fails to read from channel buffer
     */
    public static PcepLabelRange read(ChannelBuffer cb) throws PcepParseException {

        //parse and store SRP mandatory object
        PcepSrpObject srpObj = null;
        srpObj = PcepSrpObjectVer1.read(cb);
        if (srpObj == null) {
            throw new PcepParseException("Exception while parsing srp object");
        }

        LinkedList<PcepLabelRangeObject> llLabelRangeList = new LinkedList<>();
        boolean bFoundLabelRangeObj = false;
        while (0 < cb.readableBytes()) {
            //parse and store <labelrange-list>
            PcepLabelRangeObject lrObj;
            lrObj = PcepLabelRangeObjectVer1.read(cb);
            if (lrObj == null) {
                throw new PcepParseException("Exception while parsing label range object");
            } else {
                llLabelRangeList.add(lrObj);
                bFoundLabelRangeObj = true;
            }
        }

        if (!bFoundLabelRangeObj) {
            throw new PcepParseException("At least one LABEL-RANGE MUST be present.");
        }
        return new PcepLabelRangeVer1(srpObj, llLabelRangeList);
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {
        //write Object header
        int objStartIndex = cb.writerIndex();

        //write <SRP>
        int objLenIndex = srpObject.write(cb);

        if (objLenIndex <= 0) {
            throw new PcepParseException("bjectLength is " + objLenIndex);
        }

        //write <labelrange-list>
        ListIterator<PcepLabelRangeObject> listIterator = llLabelRangeList.listIterator();
        while (listIterator.hasNext()) {
            listIterator.next().write(cb);
        }

        //Update object length now
        int length = cb.writerIndex() - objStartIndex;
        // As per RFC the length of object should be
        // multiples of 4
        int pad = length % 4;
        if (pad != 0) {
            pad = 4 - pad;
            for (int i = 0; i < pad; i++) {
                cb.writeByte((byte) 0);
            }
            length = length + pad;
        }
        cb.setShort(objLenIndex, (short) length);
        return length;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("srpObject", srpObject)
                .add("LabelRangeList", llLabelRangeList)
                .toString();
    }
}
