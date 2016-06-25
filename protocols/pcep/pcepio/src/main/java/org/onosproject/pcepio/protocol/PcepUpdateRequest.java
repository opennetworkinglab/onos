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
 * Abstraction of an entity providing PCEP Update Request List.
 */
public interface PcepUpdateRequest {

    /**
     * Returns object of PCEP SRP Object.
     *
     * @return srpObject of type PCEP SRP Object
     */
    PcepSrpObject getSrpObject();

    /**
     * Returns object of PCEP LSP Object.
     *
     * @return lspObject of type PCEP LSP Object
     */
    PcepLspObject getLspObject();

    /**
     * Returns object of PCEP MSG PATH.
     *
     * @return msgPath of type PCEP MSG PATH
     */
    PcepMsgPath getMsgPath();

    /**
     * Sets the PCEP SRP Object.
     *
     * @param srpObject object of type PCEP SRP Object
     */
    void setSrpObject(PcepSrpObject srpObject);

    /**
     * Sets the PCEP LSP Object.
     *
     * @param lspObject object of type PCEP LSP Object
     */
    void setLspObject(PcepLspObject lspObject);

    /**
     * sets the PCEP MSG PATH.
     *
     * @param msgPath object of type PCEP MSG PATH
     */
    void setMsgPath(PcepMsgPath msgPath);

    /**
     * Builder interface with get and set functions to build PcepUpdateRequest.
     */
    interface Builder {

        /**
         * Builds PcepUpdateRequest.
         *
         * @return PcepUpdateRequest
         * @throws PcepParseException if mandatory object is not set
         */
        PcepUpdateRequest build() throws PcepParseException;

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
         * @param srpObj of type PcepSrpObject
         * @return builder by setting PcepSrpObject
         */
        Builder setSrpObject(PcepSrpObject srpObj);

        /**
         * Sets the LSP Object.
         *
         * @param lspObject of type PcepLspObject
         * @return builder by setting PcepLspObject
         */
        Builder setLspObject(PcepLspObject lspObject);

        /**
         * Sets the Path Object.
         *
         * @param msgPath of type PcepMsgPath
         * @return builder by setting PcepMsgPath
         */
        Builder setMsgPath(PcepMsgPath msgPath);
    }
}
