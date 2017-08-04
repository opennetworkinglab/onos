/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.driver.query;

import java.util.Set;
import java.util.stream.IntStream;

import org.onlab.packet.MplsLabel;
import org.onlab.util.GuavaCollectors;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.MplsQuery;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

/**
 * Driver which always responds that all MPLS Labels are available for the Device.
 */
public class FullMplsAvailable
        extends AbstractHandlerBehaviour
        implements MplsQuery {

    // Ref: http://www.iana.org/assignments/mpls-label-values/mpls-label-values.xhtml
    // Smallest non-reserved MPLS label
    private static final int MIN_UNRESERVED_LABEL = 0x10;
    // Max non-reserved MPLS label = 239
    private static final int MAX_UNRESERVED_LABEL = 0xEF;
    private static final Set<MplsLabel> ENTIRE_MPLS_LABELS = getEntireMplsLabels();


    @Override
    public Set<MplsLabel> queryMplsLabels(PortNumber port) {
        return ENTIRE_MPLS_LABELS;
    }

    private static Set<MplsLabel> getEntireMplsLabels() {
        return IntStream.range(MIN_UNRESERVED_LABEL, MAX_UNRESERVED_LABEL + 1)
                .mapToObj(MplsLabel::mplsLabel)
                .collect(GuavaCollectors.toImmutableSet());
    }

}
