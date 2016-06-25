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

import java.util.Optional;

import org.onlab.util.Frequency;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.DefaultAnnotations.Builder;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.optical.OmsPort;
import org.onosproject.net.optical.impl.DefaultOmsPort;
import org.slf4j.Logger;

import com.google.common.annotations.Beta;

/**
 * OMS port related helpers.
 */
@Beta
public final class OmsPortHelper {

    private static final Logger log = getLogger(OmsPortHelper.class);

    // Annotation keys
    /**
     * minFrequency in Hz.
     */
    private static final String MIN_FREQ_HZ = "minFrequency";
    /**
     * maxFrequency in Hz.
     */
    private static final String MAX_FREQ_HZ = "maxFrequency";
    /**
     * grid in Hz.
     */
    private static final String GRID_HZ = "grid";

    /**
     * Creates OMS port description based on the supplied information.
     *
     * @param number        port number
     * @param isEnabled     port enabled state
     * @param minFrequency  minimum frequency
     * @param maxFrequency  maximum frequency
     * @param grid          grid spacing frequency
     * @param annotations   key/value annotations map
     */
    public static PortDescription omsPortDescription(PortNumber number,
                                              boolean isEnabled,
                                              Frequency minFrequency,
                                              Frequency maxFrequency,
                                              Frequency grid,
                                              SparseAnnotations annotations) {

        Builder builder = DefaultAnnotations.builder();
        builder.putAll(annotations);

        builder.set(MIN_FREQ_HZ, String.valueOf(minFrequency.asHz()));
        builder.set(MAX_FREQ_HZ, String.valueOf(maxFrequency.asHz()));
        builder.set(GRID_HZ, String.valueOf(grid.asHz()));

        long portSpeed = 0;
        return new DefaultPortDescription(number, isEnabled, Port.Type.OMS, portSpeed, builder.build());
    }

    /**
     * Creates OMS port description based on the supplied information.
     *
     * @param number        port number
     * @param isEnabled     port enabled state
     * @param minFrequency  minimum frequency
     * @param maxFrequency  maximum frequency
     * @param grid          grid spacing frequency
     */
    public static PortDescription omsPortDescription(PortNumber number,
                                              boolean isEnabled,
                                              Frequency minFrequency,
                                              Frequency maxFrequency,
                                              Frequency grid) {
        return omsPortDescription(number, isEnabled, minFrequency, maxFrequency, grid, DefaultAnnotations.EMPTY);
    }

    /**
     * Creates OMS port description based on the supplied information.
     *
     * @param base          PortDescription to get basic information from
     * @param minFrequency  minimum frequency
     * @param maxFrequency  maximum frequency
     * @param grid          grid spacing frequency
     * @param annotations   key/value annotations map
     */
    public static PortDescription omsPortDescription(PortDescription base,
                                              Frequency minFrequency,
                                              Frequency maxFrequency,
                                              Frequency grid,
                                              SparseAnnotations annotations) {

        return omsPortDescription(base.portNumber(), base.isEnabled(),
                                  minFrequency, maxFrequency, grid,
                                  annotations);
    }

    public static Optional<OmsPort> asOmsPort(Port port) {
        if (port instanceof OmsPort) {
            return Optional.of((OmsPort) port);
        }

        try {
            Annotations an = port.annotations();

            Frequency minFrequency = Frequency.ofHz(Long.parseLong(an.value(MIN_FREQ_HZ)));
            Frequency maxFrequency = Frequency.ofHz(Long.parseLong(an.value(MAX_FREQ_HZ)));
            Frequency grid = Frequency.ofHz(Long.parseLong(an.value(GRID_HZ)));

            return Optional.of(new DefaultOmsPort(port, minFrequency, maxFrequency, grid));

        } catch (NumberFormatException e) {

            log.warn("{} was not well-formed OMS port.", port, e);
            return Optional.empty();
        }
    }

    // not meant to be instantiated
    private OmsPortHelper() {}
}
