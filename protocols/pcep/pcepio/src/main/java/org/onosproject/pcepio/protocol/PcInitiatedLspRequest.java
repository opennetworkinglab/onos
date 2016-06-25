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

import org.onosproject.pcepio.exceptions.PcepParseException;

/**
 * Abstraction of an entity Provides PcInitiatedLspRequest for PCEP Initiate message.
 * Reference : PCE initiated tunnel setup draft-ietf-pce-pce-initiated-lsp-03.
 */
public interface PcInitiatedLspRequest {

    /**
     * Returns object of PcepSrpObject.
     *
     * @return srpObject PCEP SRP object
     */
    PcepSrpObject getSrpObject();

    /**
     * Returns object of PcepLspObject.
     *
     * @return lspObject PCEP LSP object
     */
    PcepLspObject getLspObject();

    /**
     * Returns object of PcepEndPointsObject.
     *
     * @return endPointsObject PCEP EndPoints object
     */
    PcepEndPointsObject getEndPointsObject();

    /**
     * Returns object of PcepEroObject.
     *
     * @return eroObject PCEP ERO object
     */
    PcepEroObject getEroObject();

    /**
     * Returns object of PcepAttribute.
     *
     * @return pcepAttribute PCEP Attributes
     */
    PcepAttribute getPcepAttribute();

    /**
     * Sets PcepSrpObject.
     *
     * @param srpobj PCEP SRP object
     */
    void setSrpObject(PcepSrpObject srpobj);

    /**
     * Sets PcepLspObject.
     *
     * @param lspObject PCEP LSP object
     */
    void setLspObject(PcepLspObject lspObject);

    /**
     * Sets PcepEndPointsObject.
     *
     * @param endPointsObject PCEP EndPoints object
     */
    void setEndPointsObject(PcepEndPointsObject endPointsObject);

    /**
     * Sets PcepEroObject.
     *
     * @param eroObject PCEP ERO object
     */
    void setEroObject(PcepEroObject eroObject);

    /**
     * Sets PcepAttribute.
     *
     * @param pcepAttribute PCEP Attributes
     */
    void setPcepAttribute(PcepAttribute pcepAttribute);

    /**
     * Builder interface with get and set functions to build PcInitiatedLspRequest.
     */
    interface Builder {

        /**
         * Builds PcInitiatedLspRequest.
         *
         * @return PcInitiatedLspRequest
         * @throws PcepParseException when mandatory object is not set
         */
        PcInitiatedLspRequest build() throws PcepParseException;

        /**
         * Returns object of PcepSrpObject.
         *
         * @return srpObject
         */
        PcepSrpObject getSrpObject();

        /**
         * Returns object of PcepLspObject.
         *
         * @return lspObject
         */
        PcepLspObject getLspObject();

        /**
         * Returns object of PcepEndPointsObject.
         *
         * @return endPointsObject
         */
        PcepEndPointsObject getEndPointsObject();

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
         * Sets PcepSrpObject.
         *
         * @param srpobj PCEP SRP Object
         * @return builder by setting PcepSrpObject
         */
        Builder setSrpObject(PcepSrpObject srpobj);

        /**
         * Sets PcepLspObject.
         *
         * @param lspObject PCEP LSP Object
         * @return builder by setting PcepLspObject
         */
        Builder setLspObject(PcepLspObject lspObject);

        /**
         * Sets PcepEndPointsObject.
         *
         * @param endPointsObject EndPoints Object
         * @return builder by setting PcepEndPointsObject
         */
        Builder setEndPointsObject(PcepEndPointsObject endPointsObject);

        /**
         * Sets PcepEroObject.
         *
         * @param eroObject PCEP ERO Object
         * @return builder by setting PcepEroObject
         */
        Builder setEroObject(PcepEroObject eroObject);

        /**
         * Sets PcepAttribute.
         *
         * @param pcepAttribute PCEP Attributes
         * @return builder by setting PcepAttribute
         */
        Builder setPcepAttribute(PcepAttribute pcepAttribute);
    }
}
