/*
 * Copyright 2016-present Open Networking Laboratory
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

import java.io.IOException;
import java.util.Optional;

import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultAnnotations.Builder;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.optical.OchPort;
import org.onosproject.net.optical.impl.DefaultOchPort;
import org.onosproject.net.optical.json.OchSignalCodec;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;

/**
 * OCh port related helpers.
 */
@Beta
public final class OchPortHelper {

    private static final Logger log = getLogger(OchPortHelper.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Annotation keys
    private static final String SIGNAL_TYPE = "signalType";
    private static final String TUNABLE = "tunable";
    private static final String LAMBDA = "lambda";

    /**
     * Creates OCh port DefaultPortDescription based on the supplied information.
     *
     * @param number      port number
     * @param isEnabled   port enabled state
     * @param signalType  ODU signal type
     * @param isTunable   tunable wavelength capability
     * @param lambda      OCh signal
     * @return OCh port DefaultPortDescription with OCh annotations
     */
    public static PortDescription ochPortDescription(PortNumber number,
                                                     boolean isEnabled,
                                                     OduSignalType signalType,
                                                     boolean isTunable,
                                                     OchSignal lambda) {
        return ochPortDescription(number, isEnabled, signalType, isTunable, lambda, DefaultAnnotations.EMPTY);
    }

    /**
     * Creates OCh port DefaultPortDescription based on the supplied information.
     *
     * @param number      port number
     * @param isEnabled   port enabled state
     * @param signalType  ODU signal type
     * @param isTunable   tunable wavelength capability
     * @param lambda      OCh signal
     * @param annotationsIn key/value annotations map
     * @return OCh port DefaultPortDescription with OCh annotations
     */
    public static PortDescription ochPortDescription(PortNumber number,
                                                     boolean isEnabled,
                                                     OduSignalType signalType,
                                                     boolean isTunable,
                                                     OchSignal lambda,
                                                     SparseAnnotations annotationsIn) {

        Builder builder = DefaultAnnotations.builder();
        builder.putAll(annotationsIn);

        builder.set(TUNABLE, String.valueOf(isTunable));
        builder.set(LAMBDA, OchSignalCodec.encode(lambda).toString());
        builder.set(SIGNAL_TYPE, signalType.toString());

        DefaultAnnotations annotations = builder.build();
        long portSpeed = 0; // FIXME assign appropriate value
        return new DefaultPortDescription(number, isEnabled, Port.Type.OCH, portSpeed, annotations);
    }

    /**
     * Creates OCh port DefaultPortDescription based on the supplied information.
     *
     * @param base        PortDescription to get basic information from
     * @param signalType  ODU signal type
     * @param isTunable   tunable wavelength capability
     * @param lambda      OCh signal
     * @param annotations key/value annotations map
     * @return OCh port DefaultPortDescription with OCh annotations
     */
    public static PortDescription ochPortDescription(PortDescription base,
                                                     OduSignalType signalType,
                                                     boolean isTunable,
                                                     OchSignal lambda,
                                                     SparseAnnotations annotations) {
        return ochPortDescription(base.portNumber(), base.isEnabled(), signalType, isTunable, lambda, annotations);
    }


    public static Optional<OchPort> asOchPort(Port port) {
        if (port instanceof OchPort) {
            return Optional.of((OchPort) port);
        }

        try {
            Annotations an = port.annotations();

            OduSignalType signalType = Enum.valueOf(OduSignalType.class,
                                                    an.value(SIGNAL_TYPE));

            boolean isTunable = Boolean.valueOf(an.value(TUNABLE));

            ObjectNode obj = (ObjectNode) MAPPER.readTree(an.value(LAMBDA));
            OchSignal lambda = OchSignalCodec.decode(obj);

            // Note: OCh specific annotations is not filtered-out here.
            //       DefaultOchPort should filter them, if necessary.
            return Optional.of(new DefaultOchPort(port, signalType, isTunable, lambda));

            // TODO: it'll be better to verify each inputs properly
            // instead of catching all these Exceptions.
        } catch (IOException | NullPointerException
                | IllegalArgumentException | ClassCastException e) {

            log.warn("{} was not well-formed OCh port.", port, e);
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
        return new FilteredAnnotation(input, ImmutableSet.of(SIGNAL_TYPE, TUNABLE, LAMBDA));
    }

    // not meant to be instantiated
    private OchPortHelper() {}
}
