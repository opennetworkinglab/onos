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
package org.onosproject.pcepio.types;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepErrorObject;
import org.onosproject.pcepio.protocol.PcepOpenObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provide the error object list with open object.
 */
public class ErrorObjListWithOpen {
    //errorObjList is mandatory
    private List<PcepErrorObject> llerrorObjList;
    // openObject is optional
    private PcepOpenObject openObject;
    // flag to check if open object is set or not
    private boolean isOpenObjectSet;
    protected static final Logger log = LoggerFactory.getLogger(ErrorObjListWithOpen.class);

    /**
     * Constructor to initialize Error and OPEN object.
     *
     * @param errObj ERROR object list
     * @param openObj OPEN object
     */
    public ErrorObjListWithOpen(List<PcepErrorObject> errObj, PcepOpenObject openObj) {
        this.llerrorObjList = errObj;
        this.openObject = openObj;
        if (openObj != null) {
            isOpenObjectSet = true;
        } else {
            isOpenObjectSet = false;
        }
    }

    /**
     * Constructor to initialize ERROR Object.
     *
     * @param errObj ERROR Object list
     */
    public ErrorObjListWithOpen(List<PcepErrorObject> errObj) {
        this.llerrorObjList = errObj;
        this.openObject = null;
        isOpenObjectSet = false;
    }

    /**
     * Return list of Error Types.
     *
     * @return error types list
     */
    public List<Integer> getErrorType() {
        List<Integer> errorType = new LinkedList<>();
        if (llerrorObjList != null) {
            ListIterator<PcepErrorObject> errObjListIterator = llerrorObjList.listIterator();
            int error;
            PcepErrorObject errorObj;
            while (errObjListIterator.hasNext()) {
                errorObj = errObjListIterator.next();
                error = errorObj.getErrorType();
                errorType.add(error);
            }
        }
        return errorType;
    }

    /**
     * Return list of Error Values.
     *
     * @return error values list
     */
    public List<Integer> getErrorValue() {
        List<Integer> errorValue = new LinkedList<>();
        if (llerrorObjList != null) {
            ListIterator<PcepErrorObject> errObjListIterator = llerrorObjList.listIterator();
            int error;
            PcepErrorObject errorObj;
            while (errObjListIterator.hasNext()) {
                errorObj = errObjListIterator.next();
                error = errorObj.getErrorValue();
                errorValue.add(error);
            }
        }
        return errorValue;
    }

    /**
     * Checks whether ERROR Object list is empty or not.
     *
     * @return true if ERROR Object list is empty otherwise false
     */
    public boolean isErrorObjListWithOpenPresent() {
        // ( <error-obj-list> [<Open>]
        // At least in this case <error-obj-list> should be present.
        return !this.llerrorObjList.isEmpty();
    }

    /**
     * Write Error Object List and Open Object to channel buffer.
     *
     * @param cb output channel buffer
     * @return length of written Error object list with open
     * @throws PcepParseException when mandatory fields are not set
     */
    public int write(ChannelBuffer cb) throws PcepParseException {
        int iLenStartIndex = cb.writerIndex();
        boolean bIsErrObjListFound = false;

        //<error-obj-list> is mandatory , if not present throw exception.
        if (llerrorObjList != null) {
            ListIterator<PcepErrorObject> errObjListIterator = llerrorObjList.listIterator();
            while (errObjListIterator.hasNext()) {
                errObjListIterator.next().write(cb);
                bIsErrObjListFound = true;
            }
        }

        if (!bIsErrObjListFound) {
            throw new PcepParseException("<error-obj-list> is mandatory.");
        }

        //Open Object is optional , if present write.
        if (openObject != null) {
            openObject.write(cb);
        }

        return cb.writerIndex() - iLenStartIndex;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("ErrorObjList", llerrorObjList)
                .add("Open", openObject)
                .toString();
    }
}
