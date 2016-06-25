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
import org.onosproject.pcepio.protocol.PcepErrorInfo;
import org.onosproject.pcepio.protocol.PcepErrorObject;
import org.onosproject.pcepio.protocol.PcepRPObject;
import org.onosproject.pcepio.protocol.PcepLSObject;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP Error Info.
 * Reference : draft-dhodylee-pce-pcep-ls-01, section 8.2.
 */
public class PcepErrorInfoVer1 implements PcepErrorInfo {

    protected static final Logger log = LoggerFactory.getLogger(PcepErrorInfoVer1.class);
    //Error list is optional
    private List<PcepError> errList;

    /**
     * Constructor to add PCEP error object to the list.
     *
     * @param llRPObjList list of PCEP RP object
     * @param llLSObjList list of PCEP LS object
     * @param llErrObjList list of PCEP error object
     */
    public PcepErrorInfoVer1(List<PcepRPObject> llRPObjList, List<PcepLSObject> llLSObjList,
            List<PcepErrorObject> llErrObjList) {
        this.errList = new LinkedList<>();
        if ((llErrObjList != null) && (!llErrObjList.isEmpty())) {
            this.errList.add(new PcepErrorVer1(llRPObjList, llLSObjList, llErrObjList));
        }
    }

    /**
     * Constructor to initialize error info.
     *
     * @param errll linked list or pcep error
     */
    public PcepErrorInfoVer1(List<PcepError> errll) {
        this.errList = errll;
    }

    @Override
    public boolean isErrorInfoPresent() {
        return !this.errList.isEmpty();
    }

    @Override
    public void read(ChannelBuffer cb) throws PcepParseException {
        PcepObjectHeader tempObjHeader;

        while (0 < cb.readableBytes()) {
            cb.markReaderIndex();
            tempObjHeader = PcepObjectHeader.read(cb);
            cb.resetReaderIndex();
            byte yObjClass = tempObjHeader.getObjClass();
            if ((yObjClass != PcepRPObjectVer1.RP_OBJ_CLASS) && (yObjClass != PcepLSObjectVer1.LS_OBJ_CLASS)
                    && (yObjClass != PcepErrorObjectVer1.ERROR_OBJ_CLASS)) {
                throw new PcepParseException("Unknown Object is present in PCEP-ERROR. Object Class: " + yObjClass);
            }

            this.errList.add(PcepErrorVer1.read(cb));
        }
    }

    @Override
    public void write(ChannelBuffer cb) throws PcepParseException {
        //write <error>
        ListIterator<PcepError> listIterator = errList.listIterator();
        while (listIterator.hasNext()) {
            PcepError pcepError = listIterator.next();

            //RP Object list is optional
            List<PcepRPObject> llRPObjList = pcepError.getRPObjList();
            if (llRPObjList != null) {
                ListIterator<PcepRPObject> rpListIterator = llRPObjList.listIterator();
                while (rpListIterator.hasNext()) {
                    rpListIterator.next().write(cb);
                }
            }

            //LS Object list is optional
            List<PcepLSObject> llLSObjList = pcepError.getLSObjList();
            if (llLSObjList != null) {
                ListIterator<PcepLSObject> teListIterator = llLSObjList.listIterator();
                while (teListIterator.hasNext()) {
                    teListIterator.next().write(cb);
                }
            }

            // <error-obj-list> is mandatory
            boolean bIsErrorObjListFound = false;

            List<PcepErrorObject> llErrObjList = pcepError.getErrorObjList();
            if (llErrObjList != null) {
                ListIterator<PcepErrorObject> errObjListIterator = llErrObjList.listIterator();
                while (errObjListIterator.hasNext()) {
                    errObjListIterator.next().write(cb);
                    bIsErrorObjListFound = true;
                }
            }

            if (!bIsErrorObjListFound) {
                throw new PcepParseException("<error-obj-list> is mandatory.");
            }
        }
    }

    @Override
    public List<Integer> getErrorType() {
        List<Integer> errorType = new LinkedList<>();
        ListIterator<PcepError> listIterator = errList.listIterator();
        PcepErrorObject errObj;
        int error;
        while (listIterator.hasNext()) {
            PcepError pcepError = listIterator.next();
            List<PcepErrorObject> llErrObjList = pcepError.getErrorObjList();
            if (llErrObjList != null) {
                ListIterator<PcepErrorObject> errObjListIterator = llErrObjList.listIterator();
                while (errObjListIterator.hasNext()) {
                    errObj = errObjListIterator.next();
                    error = errObj.getErrorType();
                    errorType.add(error);
                }
            }
        }
        return errorType;
    }

    @Override
    public List<Integer> getErrorValue() {
        List<Integer> errorValue = new LinkedList<>();
        ListIterator<PcepError> listIterator = errList.listIterator();
        PcepErrorObject errObj;
        int error;
        while (listIterator.hasNext()) {
            PcepError pcepError = listIterator.next();
            List<PcepErrorObject> llErrObjList = pcepError.getErrorObjList();
            if (llErrObjList != null) {
                ListIterator<PcepErrorObject> errObjListIterator = llErrObjList.listIterator();
                while (errObjListIterator.hasNext()) {
                    errObj = errObjListIterator.next();
                    error = errObj.getErrorValue();
                    errorValue.add(error);
                }
            }
        }
        return errorValue;
    }

    /**
     * Builder class for PCEP error info.
     */
    public static class Builder implements PcepErrorInfo.Builder {
        private List<PcepError> errll;

        @Override
        public PcepErrorInfo build() {
            return new PcepErrorInfoVer1(errll);
        }

        @Override
        public List<PcepError> getPcepErrorList() {
            return this.errll;
        }

        @Override
        public Builder setPcepErrorList(List<PcepError> errll) {
            this.errll = errll;
            return this;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("ErrorList", errList).toString();
    }
}
