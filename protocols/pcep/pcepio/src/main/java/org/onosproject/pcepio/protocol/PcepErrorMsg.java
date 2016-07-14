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

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.types.ErrorObjListWithOpen;

/**
 * Abstraction of an entity providing PCEP Error Message.
 */
public interface PcepErrorMsg extends PcepMessage {

    @Override
    PcepVersion getVersion();

    @Override
    PcepType getType();

    /**
     * Returns Object of ErrorObjListWithOpen.
     *
     * @return Object of ErrorObjListWithOpen
     */
    ErrorObjListWithOpen getErrorObjListWithOpen();

    /**
     * Sets errObjListWithOpen object.
     *
     * @param errObjListWithOpen error object List with open object
     */
    void setErrorObjListWithOpen(ErrorObjListWithOpen errObjListWithOpen);

    /**
     * Returns Object of PcepErrorInfo.
     *
     * @return Object of PcepErrorInfo
     */
    PcepErrorInfo getPcepErrorInfo();

    /**
     * Sets errInfo Object.
     *
     * @param errInfo error information
     */
    void setPcepErrorInfo(PcepErrorInfo errInfo);

    @Override
    void writeTo(ChannelBuffer channelBuffer) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build PCEP Error message.
     */
    interface Builder extends PcepMessage.Builder {

        @Override
        PcepErrorMsg build();

        @Override
        PcepVersion getVersion();

        @Override
        PcepType getType();

        /**
         * Returns Object of ErrorObjListWithOpen.
         *
         * @return Object of ErrorObjListWithOpen
         */
        ErrorObjListWithOpen getErrorObjListWithOpen();

        /**
         * Sets errObjListWithOpen object.
         *
         * @param errObjListWithOpen error object with open object
         * @return builder by setting Object of ErrorObjListWithOpen
         */
        Builder setErrorObjListWithOpen(ErrorObjListWithOpen errObjListWithOpen);

        /**
         * Returns Object of PcepErrorInfo.
         *
         * @return Object of PcepErrorInfo
         */
        PcepErrorInfo getPcepErrorInfo();

        /**
         * Sets errInfo Object.
         *
         * @param errInfo error information
         * @return builder by getting Object of PcepErrorInfo
         */
        Builder setPcepErrorInfo(PcepErrorInfo errInfo);
    }
}
