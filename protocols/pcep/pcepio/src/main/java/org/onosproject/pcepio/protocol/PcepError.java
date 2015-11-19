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
package org.onosproject.pcepio.protocol;

import java.util.LinkedList;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;

/**
 * Abstraction of an entity which provides PCEP error for PCEP error message.
 */
public interface PcepError {

    /**
     * Returns the PcepRPObject List.
     *
     * @return list of type PcepRPObject
     */
    LinkedList<PcepRPObject> getRPObjList();

    /**
     * Sets the RP Objects lists.
     *
     * @param llRPObjList list of type PcepRPObject
     */
    void setRPObjList(LinkedList<PcepRPObject> llRPObjList);

    /**
     * Returns the PcepTEObject List.
     *
     * @return list of type PcepTEObject
     */
    LinkedList<PcepTEObject> getTEObjList();

    /**
     * Sets the TE Objects lists.
     *
     * @param llTEObjList list of type PcepTEObject
     */
    void setTEObjList(LinkedList<PcepTEObject> llTEObjList);

    /**
     * Returns the PcepErrorObject.
     *
     * @return list of type PcepErrorObject
     */
    LinkedList<PcepErrorObject> getErrorObjList();

    /**
     * Sets the Error Objects lists.
     *
     * @param llErrorObjList list of type PcepErrorObject
     */
    void setErrorObjList(LinkedList<PcepErrorObject> llErrorObjList);

    /**
     * Writes the byte stream of PCEP error to the channel buffer.
     *
     * @param bb of type channel buffer
     * @return object length index
     * @throws PcepParseException while writing Error part into ChannelBuffer
     */
    int write(ChannelBuffer bb) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build PcepError.
     */
    interface Builder {

        /**
         * Builds PcepError Object.
         *
         * @return PcepError Object
         */
        PcepError build();

        /**
         * Returns the PcepRPObject.
         *
         * @return list of type PcepRPObject
         */
        LinkedList<PcepRPObject> getRPObjList();

        /**
         * Sets RP Object lists and returns its builder.
         *
         * @param llRPObjList list of type PcepRpObject
         * @return builder by setting Linked list of RP Object
         */
        Builder setRPObjList(LinkedList<PcepRPObject> llRPObjList);

        /**
         * Returns the PcepTEObject.
         *
         * @return llTEObjList of type PcepTEObject
         */
        LinkedList<PcepTEObject> getTEObjList();

        /**
         * Sets TE Object lists and returns its builder.
         *
         * @param llTEObjList list of type PcepTEObject
         * @return builder by setting list of type PcepTEObject
         */
        Builder setTEObjList(LinkedList<PcepTEObject> llTEObjList);

        /**
         * Returns the PcepErrorObject.
         *
         * @return list of type PcepErrorObject
         */
        LinkedList<PcepErrorObject> getErrorObjList();

        /**
         * Sets Error Object lists and returns its builder.
         *
         * @param llErrorObjList list of type PcepErrorObject
         * @return builder by setting list of type PcepErrorObject
         */
        Builder setErrorObjList(LinkedList<PcepErrorObject> llErrorObjList);
    }
}
