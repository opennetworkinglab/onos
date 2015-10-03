/*
 * Copyright 2015 Open Networking Laboratory
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
import org.onosproject.pcepio.protocol.PcepError;
import org.onosproject.pcepio.protocol.PcepErrorObject;
import org.onosproject.pcepio.protocol.PcepRPObject;
import org.onosproject.pcepio.protocol.PcepTEObject;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PcepError list which contains RP or TE objects.
 * Reference:PCEP Extension for Transporting TE Data draft-dhodylee-pce-pcep-te-data-extn-02.
 */
public class PcepErrorVer1 implements PcepError {

    /*
           <error>::=[<request-id-list> | <te-id-list>]
                      <error-obj-list>

           <request-id-list>::=<RP>[<request-id-list>]

           <te-id-list>::=<TE>[<te-id-list>]
     */

    protected static final Logger log = LoggerFactory.getLogger(PcepErrorVer1.class);

    private boolean isErroInfoSet;
    //PcepErrorObject list
    private LinkedList<PcepErrorObject> llErrObjList;
    //PcepRPObject list
    private LinkedList<PcepRPObject> llRPObjList;
    //PcepTEObject list
    private LinkedList<PcepTEObject> llTEObjList;
    private boolean isTEObjListSet;

    public static final int OBJECT_HEADER_LENGTH = 4;

    /**
     * Constructor to initialize variable.
     */
    public PcepErrorVer1() {
        this.llRPObjList = null;
        this.llTEObjList = null;
        this.llErrObjList = null;
    }

    /**
     * Constructor to initialize variable.
     *
     * @param llRPObjList list of PcepRPObject
     * @param llTEObjList list of PcepTEObject
     * @param llErrObjListObjList list of PcepErrorObject
     */
    public PcepErrorVer1(LinkedList<PcepRPObject> llRPObjList, LinkedList<PcepTEObject> llTEObjList,
            LinkedList<PcepErrorObject> llErrObjListObjList) {
        this.llRPObjList = llRPObjList;
        this.llTEObjList = llTEObjList;
        this.llErrObjList = llErrObjListObjList;
    }

    /**
     * Constructor to initialize PcepError.
     *
     * @param llErrObjList list of PcepErrorObject
     */
    public PcepErrorVer1(LinkedList<PcepErrorObject> llErrObjList) {
        this.llRPObjList = null;
        this.llTEObjList = null;
        this.llErrObjList = llErrObjList;
    }

    @Override
    public LinkedList<PcepRPObject> getRPObjList() {
        return this.llRPObjList;
    }

    @Override
    public LinkedList<PcepTEObject> getTEObjList() {
        return this.llTEObjList;
    }

    @Override
    public LinkedList<PcepErrorObject> getErrorObjList() {
        return this.llErrObjList;
    }

    /**
     * Parse RP List from the channel buffer.
     *
     * @param cb of type channel buffer
     * @throws PcepParseException if mandatory fields are missing
     */
    public void parseRPList(ChannelBuffer cb) throws PcepParseException {
        byte yObjClass;
        byte yObjType;

        llRPObjList = new LinkedList<>();

        // caller should verify for RP object
        if (cb.readableBytes() < OBJECT_HEADER_LENGTH) {
            log.debug("Unable to find RP Object");
            return;
        }

        cb.markReaderIndex();
        PcepObjectHeader tempObjHeader = PcepObjectHeader.read(cb);
        cb.resetReaderIndex();
        yObjClass = tempObjHeader.getObjClass();
        yObjType = tempObjHeader.getObjType();
        PcepRPObject rpObj;
        while ((yObjClass == PcepRPObjectVer1.RP_OBJ_CLASS) && (yObjType == PcepRPObjectVer1.RP_OBJ_TYPE)) {
            rpObj = PcepRPObjectVer1.read(cb);
            llRPObjList.add(rpObj);

            if (cb.readableBytes() > OBJECT_HEADER_LENGTH) {
                cb.markReaderIndex();
                tempObjHeader = PcepObjectHeader.read(cb);
                cb.resetReaderIndex();
                yObjClass = tempObjHeader.getObjClass();
                yObjType = tempObjHeader.getObjType();
            } else {
                break;
            }
        }
    }

    /**
     * Parse TE List from the channel buffer.
     *
     * @param cb of type channel buffer
     * @throws PcepParseException if mandatory fields are missing
     */
    public void parseTEList(ChannelBuffer cb) throws PcepParseException {
        byte yObjClass;
        byte yObjType;

        llTEObjList = new LinkedList<>();

        // caller should verify for TE object
        if (cb.readableBytes() < OBJECT_HEADER_LENGTH) {
            log.debug("Unable to find TE Object");
            return;
        }

        cb.markReaderIndex();
        PcepObjectHeader tempObjHeader = PcepObjectHeader.read(cb);
        cb.resetReaderIndex();
        yObjClass = tempObjHeader.getObjClass();
        yObjType = tempObjHeader.getObjType();
        PcepTEObject teObj;
        while ((yObjClass == PcepTEObjectVer1.TE_OBJ_CLASS) && ((yObjType == PcepTEObjectVer1.TE_OBJ_TYPE_NODE_VALUE)
                || (yObjType == PcepTEObjectVer1.TE_OBJ_TYPE_LINK_VALUE))) {
            teObj = PcepTEObjectVer1.read(cb);
            llTEObjList.add(teObj);

            if (cb.readableBytes() > OBJECT_HEADER_LENGTH) {
                cb.markReaderIndex();
                tempObjHeader = PcepObjectHeader.read(cb);
                cb.resetReaderIndex();
                yObjClass = tempObjHeader.getObjClass();
                yObjType = tempObjHeader.getObjType();
            } else {
                break;
            }
        }
    }

    /**
     * parseErrObjList from the channel buffer.
     *
     * @param cb of type channel buffer
     * @throws PcepParseException if mandatory fields are missing
     */
    public void parseErrObjList(ChannelBuffer cb) throws PcepParseException {
        byte yObjClass;
        byte yObjType;
        boolean bIsErrorObjFound = false;

        llErrObjList = new LinkedList<>();

        // caller should verify for RP object
        if (cb.readableBytes() < OBJECT_HEADER_LENGTH) {
            throw new PcepParseException("Unable to find PCEP-ERROR Object");
        }

        cb.markReaderIndex();
        PcepObjectHeader tempObjHeader = PcepObjectHeader.read(cb);
        cb.resetReaderIndex();
        yObjClass = tempObjHeader.getObjClass();
        yObjType = tempObjHeader.getObjType();
        PcepErrorObject errorObject;
        while ((yObjClass == PcepErrorObjectVer1.ERROR_OBJ_CLASS) && (yObjType == PcepErrorObjectVer1.ERROR_OBJ_TYPE)) {
            errorObject = PcepErrorObjectVer1.read(cb);
            llErrObjList.add(errorObject);
            bIsErrorObjFound = true;

            if (cb.readableBytes() > OBJECT_HEADER_LENGTH) {
                cb.markReaderIndex();
                tempObjHeader = PcepObjectHeader.read(cb);
                cb.resetReaderIndex();
                yObjClass = tempObjHeader.getObjClass();
                yObjType = tempObjHeader.getObjType();
            } else {
                break;
            }
        }

        if (!bIsErrorObjFound) {
            throw new PcepParseException("At least one PCEP-ERROR Object should be present.");
        }
    }

    /**
     * Reads the byte stream of PcepError from channel buffer.
     *
     * @param cb of type channel buffer
     * @return PcepError error part of PCEP-ERROR
     * @throws PcepParseException if mandatory fields are missing
     */
    public static PcepErrorVer1 read(ChannelBuffer cb) throws PcepParseException {
        if (cb.readableBytes() < OBJECT_HEADER_LENGTH) {
            throw new PcepParseException("Unknown Object");
        }

        PcepErrorVer1 pcepError = new PcepErrorVer1();
        // check whether any PCEP Error Info is present
        cb.markReaderIndex();
        PcepObjectHeader tempObjHeader = PcepObjectHeader.read(cb);
        cb.resetReaderIndex();
        byte yObjClass = tempObjHeader.getObjClass();

        //If RPlist present then store it.RPList and TEList are optional
        if (yObjClass == PcepRPObjectVer1.RP_OBJ_CLASS) {
            log.debug("RP_LIST");
            pcepError.parseRPList(cb);
            yObjClass = checkNextObject(cb);
        } else if (yObjClass == PcepTEObjectVer1.TE_OBJ_CLASS) {
            log.debug("TE_LIST");
            pcepError.parseTEList(cb);
            yObjClass = checkNextObject(cb);
        }

        if (yObjClass == PcepErrorObjectVer1.ERROR_OBJ_CLASS) {
            log.debug("PCEP-ERROR obj list");
            pcepError.parseErrObjList(cb);
            yObjClass = checkNextObject(cb);
        }

        return pcepError;
    }

    /**
     * Checks Next Object.
     *
     * @param cb of type channel buffer.
     * @return object type class.
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

    /**
     * Writes the byte stream of PCEP error to the channel buffer.
     *
     * @param cb of type channel buffer
     * @return object length index
     * @throws PcepParseException if mandatory fields are missing
     */
    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {
        int iLenStartIndex = cb.writerIndex();

        // RPlist is optional
        if (this.isErroInfoSet) {
            ListIterator<PcepRPObject> rpObjlistIterator = this.llRPObjList.listIterator();
            while (rpObjlistIterator.hasNext()) {
                rpObjlistIterator.next().write(cb);
            }
        }

        // TElist is optional
        if (this.isTEObjListSet) {
            ListIterator<PcepTEObject> teObjlistIterator = this.llTEObjList.listIterator();
            while (teObjlistIterator.hasNext()) {
                teObjlistIterator.next().write(cb);
            }
        }
        //ErrList is mandatory
        ListIterator<PcepErrorObject> errlistIterator = this.llErrObjList.listIterator();
        while (errlistIterator.hasNext()) {
            errlistIterator.next().write(cb);
        }

        return cb.writerIndex() - iLenStartIndex;
    }

    /**
     * Builder for error part of PCEP-ERROR.
     */
    public static class Builder implements PcepError.Builder {

        private LinkedList<PcepRPObject> llRPObjList;
        private LinkedList<PcepTEObject> llTEObjList;
        private LinkedList<PcepErrorObject> llErrObjList;

        @Override
        public PcepError build() {
            return new PcepErrorVer1(llRPObjList, llTEObjList, llErrObjList);
        }

        @Override
        public LinkedList<PcepRPObject> getRPObjList() {
            return this.llRPObjList;
        }

        @Override
        public Builder setRPObjList(LinkedList<PcepRPObject> llRPObjList) {
            this.llRPObjList = llRPObjList;
            return this;
        }

        @Override
        public LinkedList<PcepTEObject> getTEObjList() {
            return this.llTEObjList;
        }

        @Override
        public Builder setTEObjList(LinkedList<PcepTEObject> llTEObjList) {
            this.llTEObjList = llTEObjList;
            return this;
        }

        @Override
        public LinkedList<PcepErrorObject> getErrorObjList() {
            return this.llErrObjList;
        }

        @Override
        public Builder setErrorObjList(LinkedList<PcepErrorObject> llErrObjList) {
            this.llErrObjList = llErrObjList;
            return this;
        }

    }

    @Override
    public void setRPObjList(LinkedList<PcepRPObject> llRPObjList) {
        this.llRPObjList = llRPObjList;
    }

    @Override
    public void setTEObjList(LinkedList<PcepTEObject> llTEObjList) {
        this.llTEObjList = llTEObjList;
    }

    @Override
    public void setErrorObjList(LinkedList<PcepErrorObject> llErrObjList) {
        this.llErrObjList = llErrObjList;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("RpObjectList", llRPObjList)
                .add("TeObjectList", llTEObjList)
                .add("ErrorObjectList", llErrObjList)
                .toString();
    }
}
