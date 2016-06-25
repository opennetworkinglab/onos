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
package org.onosproject.pcepio.types;

import java.util.LinkedList;

import org.onosproject.pcepio.protocol.PcepLabelObject;
import org.onosproject.pcepio.protocol.PcepLspObject;
import org.onosproject.pcepio.protocol.PcepSrpObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides Pcep Label.
 * REference :draft-zhao-pce-pcep-extension-for-pce-controller-01.
 */
public class PcepLabelDownload {

    protected static final Logger log = LoggerFactory.getLogger(PcepLabelDownload.class);

    //PCEP SPR Object
    private PcepSrpObject srpObject;
    //PCEP LSP Object
    private PcepLspObject lspObject;
    //LinkList of Labels
    private LinkedList<PcepLabelObject> llLabelList;

    /**
     * Returns SRP Object.
     *
     * @return PCEP SRP Object
     */
    public PcepSrpObject getSrpObject() {
        return srpObject;
    }

    /**
     * Sets the Pcep SRP Object.
     *
     * @param srpobj PCEP SRP Object
     */
    public void setSrpObject(PcepSrpObject srpobj) {
        this.srpObject = srpobj;
    }

    /**
     * Returns LSP Object.
     *
     * @return PCEP LSP Object
     */
    public PcepLspObject getLspObject() {
        return lspObject;
    }

    /**
     * Sets the Pcep LSP Object.
     *
     * @param lspObject PCEP LSP Object
     */
    public void setLspObject(PcepLspObject lspObject) {
        this.lspObject = lspObject;
    }

    /**
     * Returns a list of labels.
     *
     * @return llLabelList list of pcep label objects
     */
    public LinkedList<PcepLabelObject> getLabelList() {
        return llLabelList;
    }

    /**
     * set the llLabelList list of type PcepLableObject.
     *
     * @param llLabelList list of pcep label objects
     */
    public void setLabelList(LinkedList<PcepLabelObject> llLabelList) {
        this.llLabelList = llLabelList;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("SrpObject", srpObject)
                .add("LspObject", lspObject)
                .add("LabelObjectList", llLabelList)
                .toString();
    }
}
