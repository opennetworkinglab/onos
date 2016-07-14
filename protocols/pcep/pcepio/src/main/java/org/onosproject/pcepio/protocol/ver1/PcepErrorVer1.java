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
import java.util.List;
import java.util.ListIterator;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepError;
import org.onosproject.pcepio.protocol.PcepErrorObject;
import org.onosproject.pcepio.protocol.PcepRPObject;
import org.onosproject.pcepio.protocol.PcepLSObject;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PcepError list which contains RP or LS objects.
 * Reference: draft-dhodylee-pce-pcep-ls-01, section 8.2.
 */
public class PcepErrorVer1 implements PcepError {

    /*
           <error>::=[<request-id-list> | <ls-id-list>]
                      <error-obj-list>

           <request-id-list>::=<RP>[<request-id-list>]

           <ls-id-list>::=<LS>[<ls-id-list>]
     */

    protected static final Logger log = LoggerFactory.getLogger(PcepErrorVer1.class);

    private boolean isErroInfoSet;
    //PcepErrorObject list
    private List<PcepErrorObject> errObjList;
    //PcepRPObject list
    private List<PcepRPObject> rpObjList;
    //PcepLSObject list
    private List<PcepLSObject> lsObjList;
    private boolean isLSObjListSet;

    public static final int OBJECT_HEADER_LENGTH = 4;

    /**
     * Constructor to initialize variable.
     */
    public PcepErrorVer1() {
        this.rpObjList = null;
        this.lsObjList = null;
        this.errObjList = null;
    }

    /**
     * Constructor to initialize variable.
     *
     * @param rpObjList list of PcepRPObject
     * @param lsObjList list of PcepLSObject
     * @param errObjListObjList list of PcepErrorObject
     */
    public PcepErrorVer1(List<PcepRPObject> rpObjList, List<PcepLSObject> lsObjList,
            List<PcepErrorObject> errObjListObjList) {
        this.rpObjList = rpObjList;
        this.lsObjList = lsObjList;
        this.errObjList = errObjListObjList;
    }

    /**
     * Constructor to initialize PcepError.
     *
     * @param errObjList list of PcepErrorObject
     */
    public PcepErrorVer1(List<PcepErrorObject> errObjList) {
        this.rpObjList = null;
        this.lsObjList = null;
        this.errObjList = errObjList;
    }

    @Override
    public List<PcepRPObject> getRPObjList() {
        return this.rpObjList;
    }

    @Override
    public List<PcepLSObject> getLSObjList() {
        return this.lsObjList;
    }

    @Override
    public List<PcepErrorObject> getErrorObjList() {
        return this.errObjList;
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

        rpObjList = new LinkedList<>();

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
            rpObjList.add(rpObj);

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
     * Parse LS List from the channel buffer.
     *
     * @param cb of type channel buffer
     * @throws PcepParseException if mandatory fields are missing
     */
    public void parseLSList(ChannelBuffer cb) throws PcepParseException {
        byte yObjClass;
        byte yObjType;

        lsObjList = new LinkedList<>();

        // caller should verify for LS object
        if (cb.readableBytes() < OBJECT_HEADER_LENGTH) {
            log.debug("Unable to find LS Object");
            return;
        }

        cb.markReaderIndex();
        PcepObjectHeader tempObjHeader = PcepObjectHeader.read(cb);
        cb.resetReaderIndex();
        yObjClass = tempObjHeader.getObjClass();
        yObjType = tempObjHeader.getObjType();
        PcepLSObject lsObj;
        while ((yObjClass == PcepLSObjectVer1.LS_OBJ_CLASS) && ((yObjType == PcepLSObjectVer1.LS_OBJ_TYPE_NODE_VALUE)
                || (yObjType == PcepLSObjectVer1.LS_OBJ_TYPE_LINK_VALUE))) {
            lsObj = PcepLSObjectVer1.read(cb);
            lsObjList.add(lsObj);

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

        errObjList = new LinkedList<>();

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
            errObjList.add(errorObject);
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

        //If RPlist present then store it.RPList and LSList are optional
        if (yObjClass == PcepRPObjectVer1.RP_OBJ_CLASS) {
            log.debug("RP_LIST");
            pcepError.parseRPList(cb);
            yObjClass = checkNextObject(cb);
        } else if (yObjClass == PcepLSObjectVer1.LS_OBJ_CLASS) {
            log.debug("LS_LIST");
            pcepError.parseLSList(cb);
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
            ListIterator<PcepRPObject> rpObjlistIterator = this.rpObjList.listIterator();
            while (rpObjlistIterator.hasNext()) {
                rpObjlistIterator.next().write(cb);
            }
        }

        // LSlist is optional
        if (this.isLSObjListSet) {
            ListIterator<PcepLSObject> teObjlistIterator = this.lsObjList.listIterator();
            while (teObjlistIterator.hasNext()) {
                teObjlistIterator.next().write(cb);
            }
        }
        //ErrList is mandatory
        ListIterator<PcepErrorObject> errlistIterator = this.errObjList.listIterator();
        while (errlistIterator.hasNext()) {
            errlistIterator.next().write(cb);
        }

        return cb.writerIndex() - iLenStartIndex;
    }

    /**
     * Builder for error part of PCEP-ERROR.
     */
    public static class Builder implements PcepError.Builder {

        private List<PcepRPObject> rpObjList;
        private List<PcepLSObject> lsObjList;
        private List<PcepErrorObject> errObjList;

        @Override
        public PcepError build() {
            return new PcepErrorVer1(rpObjList, lsObjList, errObjList);
        }

        @Override
        public List<PcepRPObject> getRPObjList() {
            return this.rpObjList;
        }

        @Override
        public Builder setRPObjList(List<PcepRPObject> rpObjList) {
            this.rpObjList = rpObjList;
            return this;
        }

        @Override
        public List<PcepLSObject> getLSObjList() {
            return this.lsObjList;
        }

        @Override
        public Builder setLSObjList(List<PcepLSObject> lsObjList) {
            this.lsObjList = lsObjList;
            return this;
        }

        @Override
        public List<PcepErrorObject> getErrorObjList() {
            return this.errObjList;
        }

        @Override
        public Builder setErrorObjList(List<PcepErrorObject> errObjList) {
            this.errObjList = errObjList;
            return this;
        }

    }

    @Override
    public void setRPObjList(List<PcepRPObject> rpObjList) {
        this.rpObjList = rpObjList;
    }

    @Override
    public void setLSObjList(List<PcepLSObject> lsObjList) {
        this.lsObjList = lsObjList;
    }

    @Override
    public void setErrorObjList(List<PcepErrorObject> errObjList) {
        this.errObjList = errObjList;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("RpObjectList", rpObjList)
                .add("LsObjectList", lsObjList)
                .add("ErrorObjectList", errObjList)
                .toString();
    }
}
