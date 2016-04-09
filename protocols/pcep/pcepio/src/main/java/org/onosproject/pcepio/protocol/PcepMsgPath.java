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
 * Abstraction of an entity Provides PCEP Message PAth for update message.
 * Reference :PCE extensions for stateful draft-ietf-pce-stateful-pce-10.
 */
public interface PcepMsgPath {

    /**
     * Returns object of PcepEroObject.
     *
     * @return eroObject
     */
    PcepEroObject getEroObject();

    /**
     * Returns object of PcepAttribute.
     *
     * @return pcepAttribute
     */
    PcepAttribute getPcepAttribute();

    /**
     * Sets PcepEroObject.
     *
     * @param eroObject PCEP ERO Object.
     */
    void setEroObject(PcepEroObject eroObject);

    /**
     * Sets PcepAttribute.
     *
     * @param pcepAttribute PCEP-Attribute.
     */
    void setPcepAttribute(PcepAttribute pcepAttribute);

    /**
     * reads ERO object and attribute list.
     *
     * @param bb of type channel buffer
     * @return PcepMsgPath
     * @throws PcepParseException while parsing Message Path from Channel Buffer.
     */
    PcepMsgPath read(ChannelBuffer bb) throws PcepParseException;

    /**
     * writes ERO object and attribute list to channel.
     *
     * @param bb of type channel buffer
     * @return object length index
     * @throws PcepParseException while writing Message Path into Channel Buffer.
     */

    int write(ChannelBuffer bb) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build PcepMsgPath.
     */
    interface Builder {

        /**
         * Builds PcepMsgPath.
         *
         * @return PcepMsgPath
         * @throws PcepParseException when mandatory object is not set
         */
        PcepMsgPath build() throws PcepParseException;

        /**
         * Returns object of PcepEroObject.
         *
         * @return PcepEroObject
         */
        PcepEroObject getEroObject();

        /**
         * Returns object of PcepAttribute.
         *
         * @return pcepAttribute
         */
        PcepAttribute getPcepAttribute();

        /**
         * Sets PcepEroObject.
         *
         * @param eroObject PcepEroObject
         * @return Builder by setting ERO object.
         */
        Builder setEroObject(PcepEroObject eroObject);

        /**
         * Sets PcepAttribute.
         *
         * @param pcepAttribute PCEP-Attribute
         * @return Builder by setting PCEP-Attribute.
         */
        Builder setPcepAttribute(PcepAttribute pcepAttribute);
    }
}
