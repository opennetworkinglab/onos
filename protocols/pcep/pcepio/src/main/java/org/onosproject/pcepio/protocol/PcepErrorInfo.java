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

import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;

/**
 * Abstraction of an entity which provides PCEP Error Info.
 * Reference :PCEP Extension for Transporting TE Data draft-dhodylee-pce-pcep-te-data-extn-02.
 */
public interface PcepErrorInfo {

    /**
     * Returns whether error info list is present or not.
     *
     * @return true if error info present, false otherwise
     */
    boolean isErrorInfoPresent();

    /**
     * Reads from channel buffer for TE and RP objects.
     *
     * @param bb of channel buffer
     * @throws PcepParseException while parsing Error info part.
     */
    void read(ChannelBuffer bb) throws PcepParseException;

    /**
     * Writes byte stream of PCEP error info to channel buffer.
     *
     * @param bb of type channel buffer
     * @throws PcepParseException while writing Error info part into Channel Buffer.
     */
    void write(ChannelBuffer bb) throws PcepParseException;

    /**
     * Returns Error Value in PCEP-ERROR Object.
     *
     * @return list of Error Value in PCEP-ERROR Object
     */
    List<Integer> getErrorValue();

    /**
     * Returns Error Type in PCEP-ERROR Object.
     *
     * @return list of Error Type in PCEP-ERROR Object
     */
    List<Integer> getErrorType();

    /**
     * Builder interface with get and set functions to build ErrorInfo.
     */
    interface Builder {

        /**
         * Builds ErrorInfo Object.
         *
         * @return ErrorInfo Object.
         */
        PcepErrorInfo build();

        /**
         * Returns list of PcepError.
         *
         * @return list of PcepError
         */
        List<PcepError> getPcepErrorList();

        /**
         * Sets PcepError lists and returns its builder.
         *
         * @param llPcepErrorList list of PcepError
         * @return builder by setting list of PcepError.
         */
        Builder setPcepErrorList(List<PcepError> llPcepErrorList);
    }
}
