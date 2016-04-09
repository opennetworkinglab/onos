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

import java.util.LinkedList;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;

/**
 * Abstraction of an entity providing PCEP Label Range.
 */
public interface PcepLabelRange {

    /**
     * Returns object of PCEP SRP Object.
     *
     * @return srpObject
     */
    PcepSrpObject getSrpObject();

    /**
     * Sets PCEP SRP Object.
     *
     * @param srpObject SRP object.
     */
    void setSrpObject(PcepSrpObject srpObject);

    /**
     * Returns list of PcepLabelRangeObject.
     *
     * @return Label Range List
     */
    LinkedList<PcepLabelRangeObject> getLabelRangeList();

    /**
     * Sets list of PcepLabelRangeObject.
     *
     * @param llLabelRangeList Label Range List
     */
    void setLabelRangeList(LinkedList<PcepLabelRangeObject> llLabelRangeList);

    /**
     * Write the byte stream of PcepLabelRange to channel buffer.
     *
     * @param bb of type channel buffer
     * @return object length index
     * @throws PcepParseException while writing LABEL RANGE into Channel Buffer.
     */
    int write(ChannelBuffer bb) throws PcepParseException;
}
