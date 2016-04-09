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

import org.onosproject.pcepio.protocol.PcepFecObject;
import org.onosproject.pcepio.protocol.PcepLabelObject;
import org.onosproject.pcepio.protocol.PcepSrpObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provide PCEP Label Map.
 * Reference :draft-zhao-pce-pcep-extension-for-pce-controller-01.
 */
public class PcepLabelMap {

    protected static final Logger log = LoggerFactory.getLogger(PcepLabelMap.class);
    //PCEP SRP Object
    private PcepSrpObject srpObject;
    //PCEP Label Object
    private PcepLabelObject labelObject;
    //PCEP FEC Object
    private PcepFecObject fecObject;

    /**
     * Sets Fec Object.
     *
     * @param fecObject PCEP fec object
     */
    public void setFecObject(PcepFecObject fecObject) {
        this.fecObject = fecObject;
    }

    /**
     * Returns the PcepFecObject.
     *
     * @return PCEP fec object
     */
    public PcepFecObject getFecObject() {
        return this.fecObject;
    }

    /**
     * Returns SRP Object.
     *
     * @return PCEP SRP Object
     */
    public PcepSrpObject getSrpObject() {
        return srpObject;
    }

    /**
     * Sets the PCEP Srp Object.
     *
     * @param srpObject PCEP SRP Object
     */
    public void setSrpObject(PcepSrpObject srpObject) {
        this.srpObject = srpObject;
    }

    /**
     * Returns labelObject.
     *
     * @return PCEP label object
     */
    public PcepLabelObject getLabelObject() {
        return labelObject;
    }

    /**
     * Sets the Pcep labelObject.
     *
     * @param labelObject PCEP label object
     */
    public void setLabelObject(PcepLabelObject labelObject) {
        this.labelObject = labelObject;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("SrpObject", srpObject)
                .add("LabelObject", labelObject)
                .add("FecObject", fecObject)
                .toString();
    }
}
