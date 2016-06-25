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

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepAttribute;
import org.onosproject.pcepio.protocol.PcepEroObject;
import org.onosproject.pcepio.protocol.PcepMsgPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP Message PAth for update message.
 * Reference :PCE extensions for stateful draft-ietf-pce-stateful-pce-10.
 */
public class PcepMsgPathVer1 implements PcepMsgPath {

    /*
     *  <path>         ::= <ERO><attribute-list>
     */

    protected static final Logger log = LoggerFactory.getLogger(PcepMsgPathVer1.class);
    //PcepEroObject
    private PcepEroObject eroObj;
    private boolean isEroObjectSet;
    // PcepAttribute
    private PcepAttribute attrList;
    private boolean isAttributeListSet;

    /**
     * constructor to initialize objects.
     */
    public PcepMsgPathVer1() {
        eroObj = null;
        attrList = null;
        isEroObjectSet = false;
        isAttributeListSet = false;
    }

    @Override
    public PcepEroObject getEroObject() {
        return eroObj;
    }

    @Override
    public PcepAttribute getPcepAttribute() {
        return attrList;
    }

    @Override
    public void setEroObject(PcepEroObject eroObj) {
        this.eroObj = eroObj;
    }

    @Override
    public void setPcepAttribute(PcepAttribute attrList) {
        this.attrList = attrList;
    }

    /**
     * constructor to initialize member variables.
     *
     * @param eroObj pcep ero object
     * @param attrList pcep attribute
     */
    public PcepMsgPathVer1(PcepEroObject eroObj, PcepAttribute attrList) {
        this.eroObj = eroObj;
        isEroObjectSet = true;
        this.attrList = attrList;
        if (attrList == null) {
            isAttributeListSet = false;
        } else {
            isAttributeListSet = true;
        }
    }

    @Override
    public PcepMsgPath read(ChannelBuffer cb) throws PcepParseException {
        PcepEroObject eroObj;
        PcepAttribute attrList;

        eroObj = PcepEroObjectVer1.read(cb);
        attrList = PcepAttributeVer1.read(cb);

        return new PcepMsgPathVer1(eroObj, attrList);
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {
        int iLenStartIndex = cb.writerIndex();

        //write Object header
        if (this.isEroObjectSet) {
            this.eroObj.write(cb);
        }
        if (this.isAttributeListSet) {
            attrList.write(cb);
        }

        return cb.writerIndex() - iLenStartIndex;
    }

    /**
     * Builder class for PCEP Message path.
     */
    public static class Builder implements PcepMsgPath.Builder {

        private boolean bIsEroObjectSet = false;
        private boolean bIsPcepAttributeSet = false;

        //PCEP ERO Object
        private PcepEroObject eroObject;
        //PCEP Attribute list
        private PcepAttribute pcepAttribute;

        @Override
        public PcepMsgPath build() throws PcepParseException {

            //PCEP ERO Object
            PcepEroObject eroObject = null;
            //PCEP Attribute list
            PcepAttribute pcepAttribute = null;

            if (!this.bIsEroObjectSet) {
                throw new PcepParseException("ERO Object NOT Set while building PcepMsgPath.");
            } else {
                eroObject = this.eroObject;
            }
            if (!this.bIsPcepAttributeSet) {
                throw new PcepParseException("Pcep Attributes NOT Set while building PcepMsgPath.");
            } else {
                pcepAttribute = this.pcepAttribute;
            }

            return new PcepMsgPathVer1(eroObject, pcepAttribute);
        }

        @Override
        public PcepEroObject getEroObject() {
            return this.eroObject;
        }

        @Override
        public PcepAttribute getPcepAttribute() {
            return this.pcepAttribute;
        }

        @Override
        public Builder setEroObject(PcepEroObject eroObject) {
            this.eroObject = eroObject;
            this.bIsEroObjectSet = true;
            return this;
        }

        @Override
        public Builder setPcepAttribute(PcepAttribute pcepAttribute) {
            this.pcepAttribute = pcepAttribute;
            this.bIsPcepAttributeSet = true;
            return this;
        }

    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("EroObject", eroObj)
                .add("AttributeList", attrList)
                .toString();
    }
}
