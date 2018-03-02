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
package org.onosproject.net.optical.device;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Optional;

import org.onosproject.net.Annotations;
import org.onosproject.net.CltSignalType;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.DefaultAnnotations.Builder;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.optical.OduCltPort;
import org.onosproject.net.optical.impl.DefaultOduCltPort;
import org.slf4j.Logger;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;

/**
 * ODU client port related helpers.
 */
@Beta
public final class OduCltPortHelper {

    private static final Logger log = getLogger(OduCltPortHelper.class);

    // Annotation keys
    /**
     * {@link CltSignalType} as String.
     */
    private static final String SIGNAL_TYPE = "signalType";

    /**
     * Creates ODU client port description based on the supplied information.
     *
     * @param number        port number
     * @param isEnabled     port enabled state
     * @param signalType    ODU client signal type
     * @return              port description
     */
    public static PortDescription oduCltPortDescription(PortNumber number,
                                        boolean isEnabled,
                                        CltSignalType signalType) {
        return oduCltPortDescription(number, isEnabled, signalType, DefaultAnnotations.EMPTY);
    }

    /**
     * Creates ODU client port description based on the supplied information.
     *
     * @param number        port number
     * @param isEnabled     port enabled state
     * @param signalType    ODU client signal type
     * @param annotations   key/value annotations map
     * @return              port description
     */
    public static PortDescription oduCltPortDescription(PortNumber number,
                                                        boolean isEnabled,
                                                        CltSignalType signalType,
                                                        SparseAnnotations annotations) {
        Builder builder = DefaultAnnotations.builder();
        builder.putAll(annotations);

        builder.set(SIGNAL_TYPE, signalType.toString());

        long portSpeed = signalType.bitRate();
        return DefaultPortDescription.builder()
                .withPortNumber(number)
                .isEnabled(isEnabled)
                .type(Port.Type.ODUCLT)
                .portSpeed(portSpeed)
                .annotations(builder.build())
                .build();
    }

    /**
     * Creates ODU client port description based on the supplied information.
     *
     * @param base          PortDescription to get basic information from
     * @param signalType    ODU client signal type
     * @param annotations   key/value annotations map
     * @return              port description
     */
    public static PortDescription oduCltPortDescription(PortDescription base,
                                                        CltSignalType signalType,
                                                        SparseAnnotations annotations) {
        return oduCltPortDescription(base.portNumber(), base.isEnabled(), signalType, annotations);
    }

    public static Optional<OduCltPort> asOduCltPort(Port port) {
        if (port instanceof OduCltPort) {
            return Optional.of((OduCltPort) port);
        }

        try {
            Annotations an = port.annotations();

            CltSignalType signalType = Enum.valueOf(CltSignalType.class,
                                                    an.value(SIGNAL_TYPE));


            // Note: ODU specific annotations is not filtered-out here.
            //       DefaultOduCltPort should filter them, if necessary.
            return Optional.of(new DefaultOduCltPort(port, signalType));

        } catch (NullPointerException | IllegalArgumentException e) {

            log.warn("{} was not well-formed OduClt port.", port, e);
            return Optional.empty();
        }
    }

    /**
     * Returns {@link Annotations} not used by the port type projection.
     *
     * @param input {@link Annotations}
     * @return filtered view of given {@link Annotations}
     */
    public static Annotations stripHandledAnnotations(Annotations input) {
        return new FilteredAnnotation(input, ImmutableSet.of(SIGNAL_TYPE));
    }

    // not meant to be instantiated
    private OduCltPortHelper() {}
}
