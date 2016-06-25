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
import org.onosproject.pcepio.protocol.PcepBandwidthObject;
import org.onosproject.pcepio.protocol.PcepEroObject;
import org.onosproject.pcepio.protocol.PcepLspObject;
import org.onosproject.pcepio.protocol.PcepRroObject;
import org.onosproject.pcepio.protocol.PcepSrpObject;
import org.onosproject.pcepio.protocol.PcepStateReport;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

/**
 * Provide the State Report for the Pcep Report Message.
 * Reference :PCE extensions for stateful draft-ietf-pce-stateful-pce-10.
 */
public class PcepStateReportVer1 implements PcepStateReport {
    /*
     * <state-report>     ::= [<SRP>]
                               <LSP>
                               <path>
       Where:
               <path>     ::= <ERO><attribute-list>[<RRO>]
     */

    protected static final Logger log = LoggerFactory.getLogger(PcepStateReport.class);

    public static final int OBJECT_HEADER_LENGTH = 4;

    /**
     * Provides PCEP Message path for report message.
     */
    public class PcepMsgPath implements PcepStateReport.PcepMsgPath {

        /*
         * <path>                  ::= <ERO><attribute-list>[<RRO>]
         */

        //PcepEroObject
        private PcepEroObject eroObj;
        private boolean isEroObjectSet;
        //PcepAttribute List
        private PcepAttribute attrList;
        private boolean isAttributeListSet;
        //PcepRroObject
        private PcepRroObject rroObj;
        private boolean isRroObjectSet;
        private PcepBandwidthObject bandwidth;
        private boolean isBandwidthObjectSet;

        /**
         * Constructor to reset the parameters.
         */
        public PcepMsgPath() {
            eroObj = null;
            attrList = null;
            rroObj = null;
            this.isEroObjectSet = false;
            this.isAttributeListSet = false;
            this.isRroObjectSet = false;
            this.isBandwidthObjectSet = false;
        }

        /**
         * Constructor to initialize the parameters from PCEP Message path.
         *
         * @param eroObj PCEP ERO Object
         * @param attrList PCEP Attribute
         * @param rroObj PCEP Rro Object
         * @param bandwidth PCEP bandwidth object
         */
        public PcepMsgPath(PcepEroObject eroObj, PcepAttribute attrList, PcepRroObject rroObj,
                           PcepBandwidthObject bandwidth) {

            this.eroObj = eroObj;
            this.attrList = attrList;
            this.rroObj = rroObj;
            this.bandwidth = bandwidth;
            if (rroObj == null) {
                this.isRroObjectSet = false;
            } else {
                this.isRroObjectSet = true;
            }
            if (eroObj == null) {
                this.isEroObjectSet = false;
            } else {
                this.isEroObjectSet = true;
            }
            if (attrList == null) {
                this.isAttributeListSet = false;
            } else {
                this.isAttributeListSet = true;
            }
            if (bandwidth == null) {
                this.isBandwidthObjectSet = false;
            } else {
                this.isBandwidthObjectSet = true;
            }
        }

        /**
         * Returns PcepEroObject.
         *
         * @return eroObj PCEP ERO Object
         */
        @Override
        public PcepEroObject getEroObject() {
            return this.eroObj;
        }

        /**
         * Returns PCEP Attribute.
         *
         * @return attrList Attribute list
         */
        @Override
        public PcepAttribute getPcepAttribute() {
            return this.attrList;
        }

        /**
         * Returns PcepRroObject.
         *
         * @return rroObj PCEP RRO Object
         */
        @Override
        public PcepRroObject getRroObject() {
            return this.rroObj;
        }

        @Override
        public PcepBandwidthObject getBandwidthObject() {
            return this.bandwidth;
        }

        @Override
        public void setEroObject(PcepEroObject eroObj) {
            this.eroObj = eroObj;
        }

        @Override
        public void setPcepAttribute(PcepAttribute attrList) {
            this.attrList = attrList;
        }

        @Override
        public void setRroObject(PcepRroObject rroObj) {
            this.rroObj = rroObj;
        }

        @Override
        public void setBandwidthObject(PcepBandwidthObject bandwidth) {
            this.bandwidth = bandwidth;
        }

        /**
         * Reads all the Objects for PCEP Message Path.
         *
         * @param bb of type channel buffer
         * @return PCEP Message path
         * @throws PcepParseException when fails to read pcep message path
         */
        @Override
        public PcepMsgPath read(ChannelBuffer bb) throws PcepParseException {

            PcepEroObject eroObj;
            PcepAttribute attrList;
            PcepRroObject rroObj = null;
            PcepBandwidthObject bandwidth = null;

            eroObj = PcepEroObjectVer1.read(bb);
            attrList = PcepAttributeVer1.read(bb);

            boolean bBreakWhile = false;
            while (0 < bb.readableBytes()) {

                if (bb.readableBytes() < OBJECT_HEADER_LENGTH) {
                    break;
                }
                bb.markReaderIndex();
                PcepObjectHeader tempObjHeader = PcepObjectHeader.read(bb);
                bb.resetReaderIndex();
                byte yObjClass = tempObjHeader.getObjClass();

                switch (yObjClass) {
                case PcepRroObjectVer1.RRO_OBJ_CLASS:
                    rroObj = PcepRroObjectVer1.read(bb);
                    break;
                case PcepInterLayerObjectVer1.INTER_LAYER_OBJ_CLASS:
                    bb.skipBytes(tempObjHeader.getObjLen());
                    break;
                case PcepBandwidthObjectVer1.BANDWIDTH_OBJ_CLASS:
                    bandwidth = PcepBandwidthObjectVer1.read(bb);
                    break;
                default:
                    //Otherthan above objects handle those objects in caller.
                    bBreakWhile = true;
                    break;
                }
                if (bBreakWhile) {
                    break;
                }
            }
            return new PcepMsgPath(eroObj, attrList, rroObj, bandwidth);
        }

        /**
         * Writes all the objects for PCEP message path.
         *
         * @param bb of type channel buffer.
         * @return object length index
         * @throws PcepParseException when fails to write to channel buffer
         */
        @Override
        public int write(ChannelBuffer bb) throws PcepParseException {
            int iLenStartIndex = bb.writerIndex();

            //write Object header
            if (this.isEroObjectSet) {
                this.eroObj.write(bb);
            } else {
                throw new PcepParseException("Ero object is not set in path");
            }

            if (this.isAttributeListSet) {
                this.attrList.write(bb);
            }

            // RRO is optional check and read
            if (this.isRroObjectSet) {
                this.rroObj.write(bb);
                // bandwidth should come along with RRO.
                if (this.isBandwidthObjectSet) {
                    this.bandwidth.write(bb);
                }
            }
            return bb.writerIndex() - iLenStartIndex;
        }

        @Override
        public String toString() {
            ToStringHelper toStrHelper = MoreObjects.toStringHelper(getClass());

            if (attrList != null) {
                toStrHelper.add("AttributeList", attrList);
            }
            if (rroObj instanceof PcepRroObjectVer1) {
                toStrHelper.add("RroObject", rroObj);
            }
            if (bandwidth instanceof PcepBandwidthObjectVer1) {
                toStrHelper.add("bandwidthObject", bandwidth);
            }
            return toStrHelper.toString();
        }
    }

    //SRP Object
    private PcepSrpObject srpObject;
    //LSP Object
    private PcepLspObject lspObject;
    //PcepMsgPath
    private PcepStateReport.PcepMsgPath msgPath;

    /**
     * Constructor to reset objects.
     */
    public PcepStateReportVer1() {
        this.srpObject = null;
        this.lspObject = null;
        this.msgPath = null;
    }

    public PcepStateReportVer1(PcepSrpObject srpObject, PcepLspObject lspObject, PcepStateReport.PcepMsgPath msgPath) {
        this.srpObject = srpObject;
        this.lspObject = lspObject;
        this.msgPath = msgPath;
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
    public PcepStateReport.PcepMsgPath getMsgPath() {
        return msgPath;
    }

    @Override
    public void setSrpObject(PcepSrpObject srpObj) {
        this.srpObject = srpObj;
    }

    @Override
    public void setLspObject(PcepLspObject lspObject) {
        this.lspObject = lspObject;
    }

    @Override
    public void setMsgPath(PcepStateReport.PcepMsgPath msgPath) {
        this.msgPath = msgPath;
    }

    /**
     * Builder class for PCEP state report.
     */
    public static class Builder implements PcepStateReport.Builder {

        private boolean bIsSrpObjectSet = false;
        private boolean bIsLspObjectSet = false;
        private boolean bIsPcepMsgPathSet = false;

        //PCEP SRP Object
        private PcepSrpObject srpObject;
        //PCEP LSP Object
        private PcepLspObject lspObject;
        //PCEP Attribute list
        private PcepStateReport.PcepMsgPath msgPath;

        @Override
        public PcepStateReport build() throws PcepParseException {

            //PCEP SRP Object
            PcepSrpObject srpObject = null;
            //PCEP LSP Object
            PcepLspObject lspObject = null;
            //PCEP Attribute list
            PcepStateReport.PcepMsgPath msgPath = null;

            if (this.bIsSrpObjectSet) {
                srpObject = this.srpObject;
            }

            if (!this.bIsLspObjectSet) {
                throw new PcepParseException(" LSP Object NOT Set while building PcepStateReport.");
            } else {
                lspObject = this.lspObject;
            }
            if (!this.bIsPcepMsgPathSet) {
                throw new PcepParseException(" Message Path NOT Set while building PcepStateReport.");
            } else {
                msgPath = this.msgPath;
            }

            return new PcepStateReportVer1(srpObject, lspObject, msgPath);
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
        public PcepStateReport.PcepMsgPath getMsgPath() {
            return this.msgPath;
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
        public Builder setMsgPath(PcepStateReport.PcepMsgPath msgPath) {
            this.msgPath = msgPath;
            this.bIsPcepMsgPathSet = true;
            return this;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("SrpObject", srpObject)
                .add("LspObject", lspObject)
                .add("MsgPath", msgPath)
                .toString();
    }
}
