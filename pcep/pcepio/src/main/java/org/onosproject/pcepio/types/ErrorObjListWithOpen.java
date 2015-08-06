package org.onosproject.pcepio.types;

import java.util.LinkedList;
import java.util.ListIterator;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepErrorObject;
import org.onosproject.pcepio.protocol.PcepOpenObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Provide the error object list with open object.
 */
public class ErrorObjListWithOpen {
    //errorObjList is mandatory
    LinkedList<PcepErrorObject> llerrorObjList;
    // openObject is optional
    PcepOpenObject openObject;
    // flag to check if open object is set or not
    public boolean isOpenObjectSet;
    protected static final Logger log = LoggerFactory.getLogger(ErrorObjListWithOpen.class);

    /*
     * constructor to initialize errObj,openObj.
     *
     * @param errObj error object list
     * @param openObj open object
     */
    public ErrorObjListWithOpen(LinkedList<PcepErrorObject> errObj, PcepOpenObject openObj) {
        this.llerrorObjList = errObj;
        this.openObject = openObj;
        if (openObj != null) {
            isOpenObjectSet = true;
        } else {
            isOpenObjectSet = false;
        }
    }

    /*
     * constructor to initialize errObj.
     *
     * @param errObj error object list
     */
    public ErrorObjListWithOpen(LinkedList<PcepErrorObject> errObj) {
        this.llerrorObjList = errObj;
        this.openObject = null;
        isOpenObjectSet = false;
    }

    public LinkedList<Integer> getErrorType() {
        LinkedList<Integer> errorType = new LinkedList<Integer>();
        if (llerrorObjList != null) {
            ListIterator<PcepErrorObject> errObjListIterator = llerrorObjList.listIterator();
            int error;
            PcepErrorObject errorObj;
            while (errObjListIterator.hasNext()) {
                errorObj =  errObjListIterator.next();
                error = errorObj.getErrorType();
                errorType.add(error);
            }
        }
        return errorType;
    }

    public LinkedList<Integer> getErrorValue() {
        LinkedList<Integer> errorValue = new LinkedList<Integer>();
        if (llerrorObjList != null) {
            ListIterator<PcepErrorObject> errObjListIterator = llerrorObjList.listIterator();
            int error;
            PcepErrorObject errorObj;
            while (errObjListIterator.hasNext()) {
                errorObj =  errObjListIterator.next();
                error = errorObj.getErrorValue();
                errorValue.add(error);

            }
        }
        return errorValue;
    }
    /*
     * Checks whether error object list is empty or not.
     *
     * @return whether error object list is empty or not
     */
    public boolean isErrorObjListWithOpenPresent() {
        // ( <error-obj-list> [<Open>]
        // At least in this case <error-obj-list> should be present.
        return (!this.llerrorObjList.isEmpty()) ? true : false;
    }

    /*
     * Write Error Object List and Open Object to channel buffer.
     *
     * @param bb of type channel buffer
     * @throws PcepParseException when mandatory fields are not set
     */
    public int write(ChannelBuffer bb) throws PcepParseException {
        int iLenStartIndex = bb.writerIndex();
        boolean bIsErrObjListFound = false;

        //<error-obj-list> is mandatory , if not present throw exception.
        if (llerrorObjList != null) {
            ListIterator<PcepErrorObject> errObjListIterator = llerrorObjList.listIterator();
            while (errObjListIterator.hasNext()) {
                errObjListIterator.next().write(bb);
                bIsErrObjListFound = true;
            }
        }

        if (!bIsErrObjListFound) {
            throw new PcepParseException("Error: [ErrorObjListWithOpen::write] <error-obj-list> is mandatory.");
        }

        //Open Object is optional , if present write.
        if (openObject != null) {
            openObject.write(bb);
        }

        return bb.writerIndex() - iLenStartIndex;
    }

    /*
     * Prints the attributes of ErrorObject List with open Object.
     */
    public void print() {
        log.debug("ErrorObjListWithOpen:");
        ListIterator<PcepErrorObject> pcepErrorObjIterator = llerrorObjList.listIterator();
        log.debug("<error-obj-list> :");
        while (pcepErrorObjIterator.hasNext()) {
            pcepErrorObjIterator.next().print();
        }

        log.debug("OpenObject:");
        if (openObject != null) {
            openObject.print();
        }
    }
}
