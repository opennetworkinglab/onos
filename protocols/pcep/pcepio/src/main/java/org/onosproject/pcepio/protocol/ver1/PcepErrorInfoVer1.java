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
import org.onosproject.pcepio.protocol.PcepErrorInfo;
import org.onosproject.pcepio.protocol.PcepErrorObject;
import org.onosproject.pcepio.protocol.PcepRPObject;
import org.onosproject.pcepio.protocol.PcepTEObject;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP Error Info.
 * Reference :PCEP Extension for Transporting TE Data draft-dhodylee-pce-pcep-te-data-extn-02.
 */
public class PcepErrorInfoVer1 implements PcepErrorInfo {

    protected static final Logger log = LoggerFactory.getLogger(PcepErrorInfoVer1.class);
    //Error list is optional
    private LinkedList<PcepError> errList;

    /**
     * Constructor to add PCEP error object to the list.
     *
     * @param llRPObjList list of PCEP RP object
     * @param llTEObjList list of PCEP TE object
     * @param llErrObjList list of PCEP error object
     */
    public PcepErrorInfoVer1(LinkedList<PcepRPObject> llRPObjList, LinkedList<PcepTEObject> llTEObjList,
            LinkedList<PcepErrorObject> llErrObjList) {
        this.errList = new LinkedList<>();
        if ((llErrObjList != null) && (!llErrObjList.isEmpty())) {
            this.errList.add(new PcepErrorVer1(llRPObjList, llTEObjList, llErrObjList));
        }
    }

    /**
     * Constructor to initialize error info.
     *
     * @param errll linked list or pcep error
     */
    public PcepErrorInfoVer1(LinkedList<PcepError> errll) {
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
            if ((yObjClass != PcepRPObjectVer1.RP_OBJ_CLASS) && (yObjClass != PcepTEObjectVer1.TE_OBJ_CLASS)
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
            LinkedList<PcepRPObject> llRPObjList = pcepError.getRPObjList();
            if (llRPObjList != null) {
                ListIterator<PcepRPObject> rpListIterator = llRPObjList.listIterator();
                while (rpListIterator.hasNext()) {
                    rpListIterator.next().write(cb);
                }
            }

            //TE Object list is optional
            LinkedList<PcepTEObject> llTEObjList = pcepError.getTEObjList();
            if (llTEObjList != null) {
                ListIterator<PcepTEObject> teListIterator = llTEObjList.listIterator();
                while (teListIterator.hasNext()) {
                    teListIterator.next().write(cb);
                }
            }

            // <error-obj-list> is mandatory
            boolean bIsErrorObjListFound = false;

            LinkedList<PcepErrorObject> llErrObjList = pcepError.getErrorObjList();
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
    public LinkedList<Integer> getErrorType() {
        LinkedList<Integer> errorType = new LinkedList<>();
        ListIterator<PcepError> listIterator = errList.listIterator();
        PcepErrorObject errObj;
        int error;
        while (listIterator.hasNext()) {
            PcepError pcepError = listIterator.next();
            LinkedList<PcepErrorObject> llErrObjList = pcepError.getErrorObjList();
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
    public LinkedList<Integer> getErrorValue() {
        LinkedList<Integer> errorValue = new LinkedList<>();
        ListIterator<PcepError> listIterator = errList.listIterator();
        PcepErrorObject errObj;
        int error;
        while (listIterator.hasNext()) {
            PcepError pcepError = listIterator.next();
            LinkedList<PcepErrorObject> llErrObjList = pcepError.getErrorObjList();
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
        private LinkedList<PcepError> errll;

        @Override
        public PcepErrorInfo build() {
            return new PcepErrorInfoVer1(errll);
        }

        @Override
        public LinkedList<PcepError> getPcepErrorList() {
            return this.errll;
        }

        @Override
        public Builder setPcepErrorList(LinkedList<PcepError> errll) {
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
