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

package org.onosproject.pcepio.protocol;

import java.util.LinkedList;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.onosproject.pcepio.types.PcepValueType;

/**
 * Abstraction of an entity providing PCEP ERO Object.
 */
public interface PcepEroObject {

    /**
     * Return LinkedList of SubObjects of ERO Object.
     *
     * @return list of subobjects
     */
    LinkedList<PcepValueType> getSubObjects();

    /**
     * Sets LinkedList of SubObjects in ERO Object.
     *
     * @param llSubObjects list of subobjects
     */
    void setSubObjects(LinkedList<PcepValueType> llSubObjects);

    /**
     * Writes the ERO Object into channel buffer.
     *
     * @param bb channel buffer
     * @return Returns the writerIndex of this buffer
     * @throws PcepParseException while writing ERO Object into ChannelBuffer
     */
    int write(ChannelBuffer bb) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build ERO object.
     */
    interface Builder {

        /**
         * Builds ERO Object.
         *
         * @return ERO Object
         */
        PcepEroObject build();

        /**
         * Returns ERO Object Header.
         *
         * @return ERO Object Header
         */
        PcepObjectHeader getEroObjHeader();

        /**
         * Sets ERO Object header and returns its builder.
         *
         * @param obj ERO Object header
         * @return Builder by setting ERO Object header
         */
        Builder setEroObjHeader(PcepObjectHeader obj);

        /**
         * Returns LinkedList of SubObjects in ERO Objects.
         *
         * @return list of subobjects
         */
        LinkedList<PcepValueType> getSubObjects();

        /**
         * Sets LinkedList of SubObjects and returns its builder.
         *
         * @param llSubObjects list of SubObjects
         * @return Builder by setting list of SubObjects
         */
        Builder setSubObjects(LinkedList<PcepValueType> llSubObjects);

        /**
         * Sets P flag in ERO object header and returns its builder.
         *
         * @param value boolean value to set P flag
         * @return Builder by setting P flag
         */
        Builder setPFlag(boolean value);

        /**
         * Sets I flag in ERO object header and returns its builder.
         *
         * @param value boolean value to set I flag
         * @return Builder by setting I flag
         */
        Builder setIFlag(boolean value);
    }
}
