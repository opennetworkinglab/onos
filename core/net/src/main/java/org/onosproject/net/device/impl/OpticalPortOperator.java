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
package org.onosproject.net.device.impl;

import static org.onosproject.net.optical.device.OchPortHelper.ochPortDescription;
import static org.onosproject.net.optical.device.OduCltPortHelper.oduCltPortDescription;
import static org.onosproject.net.optical.device.OmsPortHelper.omsPortDescription;
import static org.onosproject.net.optical.device.OtuPortHelper.otuPortDescription;
import static org.slf4j.LoggerFactory.getLogger;
import org.onosproject.net.config.ConfigOperator;
import org.onosproject.net.config.basics.OpticalPortConfig;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.OchPortDescription;
import org.onosproject.net.device.OduCltPortDescription;
import org.onosproject.net.device.OmsPortDescription;
import org.onosproject.net.device.OtuPortDescription;
import org.onosproject.net.device.PortDescription;
import org.slf4j.Logger;

/**
 * Implementations of merge policies for various sources of optical port
 * configuration information. This includes applications, provides, and network
 * configurations.
 */
public final class OpticalPortOperator implements ConfigOperator {

    private static final Logger log = getLogger(OpticalPortOperator.class);

    private OpticalPortOperator() {
    }

    /**
     * Generates a PortDescription containing fields from a PortDescription and
     * an OpticalPortConfig.
     *
     * @param opc the port config entity from network config
     * @param descr a PortDescription
     * @return PortDescription based on both sources
     */
    public static PortDescription combine(OpticalPortConfig opc, PortDescription descr) {
        if (opc == null) {
            return descr;
        }

        PortNumber port = descr.portNumber();
        final String name = opc.name();
        final String numName = opc.numberName();
        // if the description is null, or the current description port name != config name,
        // create a new PortNumber.
        PortNumber newPort = null;
        if (port == null) {
            // try to get the portNumber from the numName.
            if (!numName.isEmpty()) {
                final long pn = Long.parseLong(numName);
                newPort = (!name.isEmpty()) ? PortNumber.portNumber(pn, name) : PortNumber.portNumber(pn);
            } else {
                // we don't have defining info (a port number value)
                throw new RuntimeException("Possible misconfig, bailing on handling for: \n\t" + descr);
            }
        } else if ((!name.isEmpty()) && !name.equals(port.name())) {
            final long pn = (numName.isEmpty()) ? port.toLong() : Long.parseLong(numName);
            newPort = PortNumber.portNumber(pn, name);
        }

        // Port type won't change unless we're overwriting a port completely.
        // Watch out for overwrites to avoid class cast craziness.
        boolean noOwrite = opc.type() == descr.type();

        SparseAnnotations sa = combine(opc, descr.annotations());
        if (noOwrite) {
            return updateDescription((newPort == null) ? port : newPort, sa, descr);
        } else {
            // TODO: must reconstruct a different type of PortDescription.
            log.info("Type rewrite from {} to {} required", descr.type(), opc.type());
        }
        return descr;
    }

    // updates a port description whose port type has not changed.
    /**
     * Updates {@link PortDescription} using specified number and annotations.
     *
     * @param port {@link PortNumber} to use in updated description
     * @param sa   annotations to use in updated description
     * @param descr base {@link PortDescription}
     * @return updated {@link PortDescription}
     */
    private static PortDescription updateDescription(PortNumber port,
                                                     SparseAnnotations sa,
                                                     PortDescription descr) {

        // TODO This switch can go away once deprecation is complete.
        switch (descr.type()) {
            case OMS:
                if (descr instanceof OmsPortDescription) {
                    OmsPortDescription oms = (OmsPortDescription) descr;
                    return omsPortDescription(port, oms.isEnabled(), oms.minFrequency(),
                                                  oms.maxFrequency(), oms.grid(), sa);
                }
                break;
            case OCH:
                // We might need to update lambda below with STATIC_LAMBDA.
                if (descr instanceof OchPortDescription) {
                    OchPortDescription och = (OchPortDescription) descr;
                    return ochPortDescription(port, och.isEnabled(), och.signalType(),
                            och.isTunable(), och.lambda(), sa);
                }
                break;
            case ODUCLT:
                if (descr instanceof OduCltPortDescription) {
                    OduCltPortDescription odu = (OduCltPortDescription) descr;
                    return oduCltPortDescription(port, odu.isEnabled(), odu.signalType(), sa);
                }
                break;
            case PACKET:
            case FIBER:
            case COPPER:
                break;
            case OTU:
                if (descr instanceof OtuPortDescription) {
                    OtuPortDescription otu = (OtuPortDescription) descr;
                    return otuPortDescription(port, otu.isEnabled(), otu.signalType(), sa);
                }
                break;
            default:
                log.warn("Unsupported optical port type {} - can't update", descr.type());
                return descr;
        }
        if (port.exactlyEquals(descr.portNumber()) && sa.equals(descr.annotations())) {
            // result is no-op
            return descr;
        }
        return new DefaultPortDescription(port,
                                          descr.isEnabled(),
                                          descr.type(),
                                          descr.portSpeed(),
                                          sa);
    }

    /**
     * Generates an annotation from an existing annotation and OptcalPortConfig.
     *
     * @param opc the port config entity from network config
     * @param an the annotation
     * @return annotation combining both sources
     */
    public static SparseAnnotations combine(OpticalPortConfig opc, SparseAnnotations an) {
        DefaultAnnotations.Builder b = DefaultAnnotations.builder();
        if (!opc.staticPort().isEmpty()) {
            b.set(AnnotationKeys.STATIC_PORT, opc.staticPort());
        }
        if (opc.staticLambda().isPresent()) {
            b.set(AnnotationKeys.STATIC_LAMBDA, String.valueOf(opc.staticLambda().get()));
        }
        // The following may not need to be carried.
        if (!opc.name().isEmpty()) {
            b.set(AnnotationKeys.PORT_NAME, opc.name());
        }
        return DefaultAnnotations.union(an, b.build());
    }

}
