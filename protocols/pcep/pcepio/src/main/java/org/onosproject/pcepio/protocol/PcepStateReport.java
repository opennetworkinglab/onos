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

/**
 * Abstraction of an entity provides State Report for PCEP Report Message.
 */
public interface PcepStateReport {

    /**
     * Provides PCEP Message path for report message.
     */
    interface PcepMsgPath {

        /**
         * Returns PcepEroObject.
         *
         * @return eroObj
         */
        PcepEroObject getEroObject();

        /**
         * Sets PcepEroObject.
         *
         * @param eroObject Ero Object
         */
        void setEroObject(PcepEroObject eroObject);

        /**
         * Returns PcepAttribute.
         *
         * @return attrList
         */
        PcepAttribute getPcepAttribute();

        /**
         * Sets PcepAttribute.
         *
         * @param pcepAttribute Pcep Attribute object
         */
        void setPcepAttribute(PcepAttribute pcepAttribute);

        /**
         * Returns PcepRroObject.
         *
         * @return rroObj
         */
        PcepRroObject getRroObject();

        /**
         * Sets PcepRroObject.
         *
         * @param rroObject Rro object
         */
        void setRroObject(PcepRroObject rroObject);

        /**
         * Returns PcepBandwidthObject.
         *
         * @return bandwidth object
         */
        PcepBandwidthObject getBandwidthObject();

        /**
         * Sets PcepBandwidthObject.
         *
         * @param bandwidth bandwidth object
         */
        void setBandwidthObject(PcepBandwidthObject bandwidth);

        /**
         * Reads all the Objects for PCEP Message Path.
         *
         * @param bb of type channel buffer
         * @return PCEP Message path
         * @throws PcepParseException when invalid buffer received
         */
        PcepMsgPath read(ChannelBuffer bb) throws PcepParseException;

        /**
         * Writes all the objects for pcep message path.
         *
         * @param bb of type channel buffer.
         * @return object length index
         * @throws PcepParseException when mandatory object is not set
         */
        int write(ChannelBuffer bb) throws PcepParseException;
    }

    /**
     * Returns PcepSrpObject.
     *
     * @return srpObject
     */
    PcepSrpObject getSrpObject();

    /**
     * Returns PcepLspObject.
     *
     * @return lspObject
     */
    PcepLspObject getLspObject();

    /**
     * Returns PcepMsgPath.
     *
     * @return msgPath
     */
    PcepMsgPath getMsgPath();

    /**
     * Sets the SRP Object.
     *
     * @param srpObj Pcep Srp Object
     */
    void setSrpObject(PcepSrpObject srpObj);

    /**
     * Sets the LSP Object.
     *
     * @param lspObject Pcep Lsp Object
     */
    void setLspObject(PcepLspObject lspObject);

    /**
     * Sets the Path Object.
     *
     * @param msgPath Pcep MsgPath object
     */
    void setMsgPath(PcepMsgPath msgPath);

    /**
     * Builder interface with get and set functions to build PcepStateReport.
     */
    interface Builder {

        /**
         * Builds PcepStateReport.
         *
         * @return PcepStateReport
         * @throws PcepParseException when mandatory object is not set
         */
        PcepStateReport build() throws PcepParseException;

        /**
         * Returns PcepSrpObject.
         *
         * @return srpObject
         */
        PcepSrpObject getSrpObject();

        /**
         * Returns PcepLspObject.
         *
         * @return lspObject
         */
        PcepLspObject getLspObject();

        /**
         * Returns PcepMsgPath.
         *
         * @return msgPath
         */
        PcepMsgPath getMsgPath();

        /**
         * Sets the SRP Object.
         *
         * @param srpObj Pcep Srp Object
         * @return builder by setting PcepSrpObject
         */
        Builder setSrpObject(PcepSrpObject srpObj);

        /**
         * Sets the LSP Object.
         *
         * @param lspObject Pcep Lsp Object
         * @return builder by setting PcepLspObject
         */
        Builder setLspObject(PcepLspObject lspObject);

        /**
         * Sets the Path Object.
         *
         * @param msgPath Pcep MsgPath object
         * @return builder by setting PcepMsgPath
         */
        Builder setMsgPath(PcepMsgPath msgPath);
    }
}
