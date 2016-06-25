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
import org.onosproject.pcepio.types.PcepLabelDownload;
import org.onosproject.pcepio.types.PcepLabelMap;

/***
 * Abstraction to provide PCEP Label Updates.
 */
public interface PcepLabelUpdate {

    /**
     * Writes the byte stream of PcepLabelUpdate into channel buffer.
     *
     * @param bb of type channel buffer
     * @throws PcepParseException while writing LABEL UPDATE.
     */
    void write(ChannelBuffer bb) throws PcepParseException;

    /**
     * Sets the Label Download object.
     *
     * @param labelDownload PCEP Label Download object
     */
    void setLabelDownload(PcepLabelDownload labelDownload);

    /**
     * Returns the PcepLabelDownload object.
     *
     * @return labelDownload PCEP Label Download
     */
    PcepLabelDownload getLabelDownload();

    /**
     * Sets the Label map object.
     *
     * @param labelMap PCEP Label Map object
     */
    void setLabelMap(PcepLabelMap labelMap);

    /**
     * Returns the PcepLabelMap object.
     *
     * @return labelMap PCEP Label Map
     */
    PcepLabelMap getLabelMap();

    /**
     * Builder interface with get and set functions to build Label Update message.
     */
    interface Builder {

        /**
         * Builds PcepLableUpdate Object.
         *
         * @return PcepLableUpdate Object
         * @throws PcepParseException while building LABEL-UPDATE.
         */
        PcepLabelUpdate build() throws PcepParseException;

        /**
         * Sets the Label Download object.
         *
         * @param labelDownload PCEP Label Download object
         * @return Builder by setting labelDownload object
         */
        Builder setLabelDownload(PcepLabelDownload labelDownload);

        /**
         * Returns the PcepLabelDownload object.
         *
         * @return labelDownload PCEP Label Download
         */
        PcepLabelDownload getLabelDownload();

        /**
         * Sets the Label map object.
         *
         * @param labelMap PCEP Label Map object
         * @return Builder by setting PcepLabelMap object
         */
        Builder setLabelMap(PcepLabelMap labelMap);

        /**
         * Returns the PcepLabelMap object.
         *
         * @return labelMap PCEP Label Map
         */
        PcepLabelMap getLabelMap();
    }

}
