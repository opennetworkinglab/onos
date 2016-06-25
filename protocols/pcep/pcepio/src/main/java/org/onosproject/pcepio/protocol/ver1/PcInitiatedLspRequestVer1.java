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

import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcInitiatedLspRequest;
import org.onosproject.pcepio.protocol.PcepAttribute;
import org.onosproject.pcepio.protocol.PcepEndPointsObject;
import org.onosproject.pcepio.protocol.PcepEroObject;
import org.onosproject.pcepio.protocol.PcepLspObject;
import org.onosproject.pcepio.protocol.PcepSrpObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PcInitiatedLspRequest for PCEP Initiate message.
 * Reference : PCE initiated tunnel setup draft-ietf-pce-pce-initiated-lsp-03.
 */
public class PcInitiatedLspRequestVer1 implements PcInitiatedLspRequest {

    /*
     * <PCE-initiated-lsp-request>       ::= (<PCE-initiated-lsp-instantiation>|<PCE-initiated-lsp-deletion>)
       <PCE-initiated-lsp-instantiation> ::= <SRP>
                                             <LSP>
                                             <END-POINTS>
                                             <ERO>
                                             [<attribute-list>]
            <PCE-initiated-lsp-deletion> ::= <SRP>
                                             <LSP>
     */

    protected static final Logger log = LoggerFactory.getLogger(PcInitiatedLspRequestVer1.class);

    //PCEP SRP Object
    private PcepSrpObject srpObject;
    //PCEP LSP Object
    private PcepLspObject lspObject;
    //PCEP End Point Object
    private PcepEndPointsObject endPointsObject;
    //PCEP ERO Object
    private PcepEroObject eroObject;
    //PCEP Attribute list
    private PcepAttribute pcepAttribute;

    /**
     * Default constructor.
     */
    public PcInitiatedLspRequestVer1() {
        srpObject = null;
        lspObject = null;
        endPointsObject = null;
        eroObject = null;
        pcepAttribute = null;

    }

    /**
     * Constructor to initialize all parameters of PC initiated lsp request.
     *
     * @param srpObject PCEP srp Object
     * @param lspObject PCEP lsp object
     * @param endPointsObject PCPE endpoints object
     * @param eroObject PCEP ero object
     * @param pcepAttribute PCEP attribute
     */
    public PcInitiatedLspRequestVer1(PcepSrpObject srpObject, PcepLspObject lspObject,
            PcepEndPointsObject endPointsObject, PcepEroObject eroObject, PcepAttribute pcepAttribute) {
        this.srpObject = srpObject;
        this.lspObject = lspObject;
        this.endPointsObject = endPointsObject;
        this.eroObject = eroObject;
        this.pcepAttribute = pcepAttribute;

    }

    @Override
    public PcepSrpObject getSrpObject() {
        return srpObject;
    }

    @Override
    public PcepLspObject getLspObject() {
        return lspObject;
    }

    @Override
    public PcepEndPointsObject getEndPointsObject() {
        return endPointsObject;
    }

    @Override
    public PcepEroObject getEroObject() {
        return eroObject;
    }

    @Override
    public PcepAttribute getPcepAttribute() {
        return pcepAttribute;
    }

    @Override
    public void setSrpObject(PcepSrpObject srpobj) {
        this.srpObject = srpobj;

    }

    @Override
    public void setLspObject(PcepLspObject lspObject) {
        this.lspObject = lspObject;
    }

    @Override
    public void setEndPointsObject(PcepEndPointsObject endPointsObject) {
        this.endPointsObject = endPointsObject;
    }

    @Override
    public void setEroObject(PcepEroObject eroObject) {
        this.eroObject = eroObject;
    }

    @Override
    public void setPcepAttribute(PcepAttribute pcepAttribute) {
        this.pcepAttribute = pcepAttribute;
    }

    /**
     * Builder class for PC initiated lsp reuqest.
     */
    public static class Builder implements PcInitiatedLspRequest.Builder {

        private boolean bIsSrpObjectSet = false;
        private boolean bIsLspObjectSet = false;
        private boolean bIsEndPointsObjectSet = false;
        private boolean bIsEroObjectSet = false;
        private boolean bIsPcepAttributeSet = false;
        private boolean bIsbRFlagSet = false;

        //PCEP SRP Object
        private PcepSrpObject srpObject;
        //PCEP LSP Object
        private PcepLspObject lspObject;
        //PCEP End Point Object
        private PcepEndPointsObject endPointsObject;
        //PCEP ERO Object
        private PcepEroObject eroObject;
        //PCEP Attribute list
        private PcepAttribute pcepAttribute;

        @Override
        public PcInitiatedLspRequest build() throws PcepParseException {

            //PCEP SRP Object
            PcepSrpObject srpObject = null;
            //PCEP LSP Object
            PcepLspObject lspObject = null;
            //PCEP End Point Object
            PcepEndPointsObject endPointsObject = null;
            //PCEP ERO Object
            PcepEroObject eroObject = null;
            //PCEP Attribute list
            PcepAttribute pcepAttribute = null;
            boolean bRFlag = false;

            if (!this.bIsSrpObjectSet) {
                throw new PcepParseException("Srp object NOT Set while building PcInitiatedLspRequest");
            } else {
                srpObject = this.srpObject;
                bRFlag = srpObject.getRFlag();
            }

            if (bRFlag) {
                this.bIsbRFlagSet = true;
            } else {
                this.bIsbRFlagSet = false;
            }

            if (!this.bIsLspObjectSet) {
                throw new PcepParseException("LSP Object NOT Set while building PcInitiatedLspRequest");
            } else {
                lspObject = this.lspObject;
            }
            if (!this.bIsbRFlagSet) {

                if (!this.bIsEndPointsObjectSet) {
                    throw new PcepParseException("EndPoints Object NOT Set while building PcInitiatedLspRequest");
                } else {
                    endPointsObject = this.endPointsObject;
                }
                if (!this.bIsEroObjectSet) {
                    throw new PcepParseException("ERO Object NOT Set while building PcInitiatedLspRequest");
                } else {
                    eroObject = this.eroObject;
                }
                if (bIsPcepAttributeSet) {
                    pcepAttribute = this.pcepAttribute;
                }
            }
            return new PcInitiatedLspRequestVer1(srpObject, lspObject, endPointsObject, eroObject, pcepAttribute);
        }

        @Override
        public PcepSrpObject getSrpObject() {
            return this.srpObject;
        }

        @Override
        public PcepLspObject getLspObject() {
            return this.lspObject;
        }

        @Override
        public PcepEndPointsObject getEndPointsObject() {
            return this.endPointsObject;
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
        public Builder setSrpObject(PcepSrpObject srpobj) {
            this.srpObject = srpobj;
            this.bIsSrpObjectSet = true;
            return this;

        }

        @Override
        public Builder setLspObject(PcepLspObject lspObject) {
            this.lspObject = lspObject;
            this.bIsLspObjectSet = true;
            return this;
        }

        @Override
        public Builder setEndPointsObject(PcepEndPointsObject endPointsObject) {
            this.endPointsObject = endPointsObject;
            this.bIsEndPointsObjectSet = true;
            return this;
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
                .omitNullValues()
                .add("SrpObject", srpObject)
                .add("LspObject", lspObject)
                .add("EndPointObject", endPointsObject)
                .add("EroObject", eroObject)
                .add("PcepAttribute", pcepAttribute)
                .toString();
    }
}
