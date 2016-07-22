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

import java.util.LinkedList;
import java.util.ListIterator;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepFecObject;
import org.onosproject.pcepio.protocol.PcepLabelObject;
import org.onosproject.pcepio.protocol.PcepLabelUpdate;
import org.onosproject.pcepio.protocol.PcepLspObject;
import org.onosproject.pcepio.protocol.PcepSrpObject;
import org.onosproject.pcepio.types.PcepLabelDownload;
import org.onosproject.pcepio.types.PcepLabelMap;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP LABEL update .
 * Reference :draft-zhao-pce-pcep-extension-for-pce-controller-01.
 */
public class PcepLabelUpdateVer1 implements PcepLabelUpdate {

    /*
     *       <pce-label-update>      ::= (<pce-label-download>|<pce-label-map>)

            Where:
             <pce-label-download>    ::= <SRP>
                                         <LSP>
                                         <label-list>

             <pce-label-map>         ::= <SRP>
                                         <LABEL>
                                         <FEC>

             <label-list >           ::= <LABEL>
                                         [<label-list>]
     */
    protected static final Logger log = LoggerFactory.getLogger(PcepLabelUpdateVer1.class);

    //Either PceLabelDownload or PceLabelMap is mandatory.
    //label Download
    private PcepLabelDownload labelDownload;
    private boolean isLabelDownloadSet;
    //label Map
    private PcepLabelMap labelMap;
    private boolean isLabelMapSet;

    /**
     * Constructor to reset parameters.
     */
    public PcepLabelUpdateVer1() {
        this.labelDownload = null;
        this.isLabelDownloadSet = false;
        this.labelMap = null;
        this.isLabelMapSet = false;
    }

    /**
     * Constructor to initialize PCEP label download.
     *
     * @param labelDownload PCEP label download
     */
    public PcepLabelUpdateVer1(PcepLabelDownload labelDownload) {
        this.labelDownload = labelDownload;
        this.isLabelDownloadSet = true;
        this.labelMap = null;
        this.isLabelMapSet = false;
    }

    /**
     * Constructor to initialize PCEP label map.
     *
     * @param labelMap PCEP label map
     */
    public PcepLabelUpdateVer1(PcepLabelMap labelMap) {
        this.labelDownload = null;
        this.isLabelDownloadSet = false;
        this.labelMap = labelMap;
        this.isLabelMapSet = true;
    }

    /**
     * builder class for PCEP label update.
     */
    static class Builder implements PcepLabelUpdate.Builder {

        private PcepLabelDownload labelDownload;
        private boolean isLabelDownloadSet;
        private PcepLabelMap labelMap;
        private boolean isLabelMapSet;

        @Override
        public PcepLabelUpdate build() throws PcepParseException {

            if (isLabelDownloadSet) {
                return new PcepLabelUpdateVer1(labelDownload);
            }
            if (isLabelMapSet) {
                return new PcepLabelUpdateVer1(labelMap);
            }
            if (!isLabelDownloadSet && !isLabelMapSet) {
                throw new PcepParseException(
                        "Label Download or Label Map is not set while building PcepLabelUpdate Message");
            }
            return new PcepLabelUpdateVer1();
        }

        @Override
        public Builder setLabelDownload(PcepLabelDownload labelDownload) {
            this.labelDownload = labelDownload;
            this.isLabelDownloadSet = true;
            return this;
        }

        @Override
        public PcepLabelDownload getLabelDownload() {
            return labelDownload;
        }

        @Override
        public Builder setLabelMap(PcepLabelMap labelMap) {
            this.labelMap = labelMap;
            this.isLabelMapSet = true;
            return this;
        }

        @Override
        public PcepLabelMap getLabelMap() {
            return labelMap;
        }
    }

    /**
     * Reads PcepLabels from the byte stream received from channel buffer.
     *
     * @param cb of type channel buffer.
     * @return PcepLabelUpdate object.
     * @throws PcepParseException when fails to read from channel buffer
     */
    public static PcepLabelUpdate read(ChannelBuffer cb) throws PcepParseException {

        PcepLabelUpdateVer1 pceLabelUpdate = new PcepLabelUpdateVer1();

        PcepSrpObject srpObject;
        PcepObjectHeader tempObjHeader;

        //read SRP mandatory Object
        srpObject = PcepSrpObjectVer1.read(cb);

        //checking next object
        cb.markReaderIndex();

        tempObjHeader = PcepObjectHeader.read(cb);
        cb.resetReaderIndex();

        if (tempObjHeader.getObjClass() == PcepLspObjectVer1.LSP_OBJ_CLASS) {

            //now it is belong to <pce-label-download>
            PcepLabelDownload labelDownload = new PcepLabelDownload();

            //set SRP
            labelDownload.setSrpObject(srpObject);

            //read and set LSP
            labelDownload.setLspObject(PcepLspObjectVer1.read(cb));

            //<label-list>
            LinkedList<PcepLabelObject> llLabelList = new LinkedList<>();
            PcepLabelObject labelObject;

            while (0 < cb.readableBytes()) {

                cb.markReaderIndex();
                tempObjHeader = PcepObjectHeader.read(cb);
                cb.resetReaderIndex();

                if (tempObjHeader.getObjClass() != PcepLabelObjectVer1.LABEL_OBJ_CLASS) {
                    break;
                }
                labelObject = PcepLabelObjectVer1.read(cb);
                llLabelList.add(labelObject);
            }
            labelDownload.setLabelList(llLabelList);
            pceLabelUpdate.setLabelDownload(labelDownload);
        } else if (tempObjHeader.getObjClass() == PcepLabelObjectVer1.LABEL_OBJ_CLASS) {
            //belong to <pce-label-map>
            PcepLabelMap labelMap = new PcepLabelMap();

            //set SRP Object
            labelMap.setSrpObject(srpObject);

            //read and set Label Object
            labelMap.setLabelObject(PcepLabelObjectVer1.read(cb));

            cb.markReaderIndex();
            tempObjHeader = PcepObjectHeader.read(cb);
            cb.resetReaderIndex();

            PcepFecObject fecObject = null;
            switch (tempObjHeader.getObjType()) {
            case PcepFecObjectIPv4Ver1.FEC_OBJ_TYPE:
                fecObject = PcepFecObjectIPv4Ver1.read(cb);
                break;
            case PcepFecObjectIPv6Ver1.FEC_OBJ_TYPE:
                fecObject = PcepFecObjectIPv6Ver1.read(cb);
                break;
            case PcepFecObjectIPv4AdjacencyVer1.FEC_OBJ_TYPE:
                fecObject = PcepFecObjectIPv4AdjacencyVer1.read(cb);
                break;
            case PcepFecObjectIPv6AdjacencyVer1.FEC_OBJ_TYPE:
                fecObject = PcepFecObjectIPv6AdjacencyVer1.read(cb);
                break;
            case PcepFecObjectIPv4UnnumberedAdjacencyVer1.FEC_OBJ_TYPE:
                fecObject = PcepFecObjectIPv4UnnumberedAdjacencyVer1.read(cb);
                break;
            default:
                throw new PcepParseException("Unkown FEC object type " + tempObjHeader.getObjType());
            }
            labelMap.setFecObject(fecObject);
            pceLabelUpdate.setLabelMap(labelMap);
        } else {
            throw new PcepParseException(
                    "Either <pce-label-download> or <pce-label-map> should be present. Received Class: "
                            + tempObjHeader.getObjClass());
        }
        return pceLabelUpdate;
    }

    @Override
    public void write(ChannelBuffer cb) throws PcepParseException {

        if ((labelDownload != null) && (labelMap != null)) {
            throw new PcepParseException("Label Download and Label Map both can't be present.");
        }

        if ((labelDownload == null) && (labelMap == null)) {
            throw new PcepParseException("Either Label Download or Label Map should be present.");
        }

        if (labelDownload != null) {

            PcepLspObject lspObject;
            PcepSrpObject srpObject;
            PcepLabelObject labelObject;
            LinkedList<PcepLabelObject> llLabelList;

            srpObject = labelDownload.getSrpObject();
            if (srpObject == null) {
                throw new PcepParseException("SRP Object is mandatory object for Label Download.");
            } else {
                srpObject.write(cb);
            }

            lspObject = labelDownload.getLspObject();
            if (lspObject == null) {
                throw new PcepParseException("LSP Object is mandatory object for Label Download.");
            } else {
                lspObject.write(cb);
            }

            llLabelList = labelDownload.getLabelList();
            if (llLabelList == null || llLabelList.isEmpty()) {
                throw new PcepParseException("Label list is mandatory object for Label Download.");
            } else {
                ListIterator<PcepLabelObject> listIterator = llLabelList.listIterator();
                while (listIterator.hasNext()) {
                    labelObject = listIterator.next();
                    labelObject.write(cb);
                }
            }
        }

        if (labelMap != null) {

            PcepSrpObject srpObject;
            PcepLabelObject labelObject;
            PcepFecObject fecObject;

            srpObject = labelMap.getSrpObject();
            if (srpObject == null) {
                throw new PcepParseException("SRP Object is mandatory object for Label map.");
            } else {
                srpObject.write(cb);
            }
            labelObject = labelMap.getLabelObject();
            if (labelObject == null) {
                throw new PcepParseException("label Object is mandatory object for Label map.");
            } else {
                labelObject.write(cb);
            }
            fecObject = labelMap.getFecObject();
            if (fecObject == null) {
                throw new PcepParseException("fec Object is mandatory object for Label map.");
            } else {
                fecObject.write(cb);
            }
        }
    }

    @Override
    public void setLabelDownload(PcepLabelDownload labelDownload) {
        if (this.isLabelMapSet) {
            return;
        }
        this.labelDownload = labelDownload;
        this.isLabelDownloadSet = true;
    }

    @Override
    public PcepLabelDownload getLabelDownload() {
        return this.labelDownload;
    }

    @Override
    public void setLabelMap(PcepLabelMap labelMap) {
        if (this.isLabelDownloadSet) {
            return;
        }
        this.labelMap = labelMap;
        this.isLabelMapSet = true;
    }

    @Override
    public PcepLabelMap getLabelMap() {
        return this.labelMap;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("LabelDownload", labelDownload)
                .add("LabelMap", labelMap)
                .toString();
    }
}
