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
package org.onosproject.net.optical.config;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Optional;
import java.util.Set;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;

import org.onosproject.net.config.Config;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.PortConfigOperator;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Port;
import org.onosproject.net.Port.Type;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.PortDescription;
import org.slf4j.Logger;

import com.google.common.collect.Sets;

/**
 * Implementations of merge policies for various sources of optical port
 * configuration information. This includes applications, provides, and network
 * configurations.
 */
public final class OpticalPortOperator implements PortConfigOperator {

    private static final Logger log = getLogger(OpticalPortOperator.class);

    /**
     * Port.Type this PortConfigOperator reacts on.
     */
    private final Set<Port.Type> optical = Sets.immutableEnumSet(Port.Type.ODUCLT,
                                                                 Port.Type.OMS,
                                                                 Port.Type.OCH,
                                                                 Port.Type.OTU,
                                                                 Port.Type.FIBER,
                                                                 Port.Type.PACKET);

    private NetworkConfigService networkConfigService;


    public OpticalPortOperator() {
    }

    @Override
    public void bindService(NetworkConfigService networkConfigService) {
        this.networkConfigService = networkConfigService;
    }

    private OpticalPortConfig lookupConfig(ConnectPoint cp) {
        if (networkConfigService == null) {
            return null;
        }
        return networkConfigService.getConfig(cp, OpticalPortConfig.class);
    }

    /**
     * Generates a PortDescription containing fields from a PortDescription and
     * an OpticalPortConfig.
     *
     * @param cp {@link ConnectPoint} representing the port.
     * @param descr input {@link PortDescription}
     * @return Combined {@link PortDescription}
     */
    @Override
    public PortDescription combine(ConnectPoint cp, PortDescription descr) {
        checkNotNull(cp);

        // short-circuit for non-optical ports
        // must be removed if we need type override
        if (descr != null && !optical.contains(descr.type())) {
            return descr;
        }

        OpticalPortConfig opc = lookupConfig(cp);
        if (descr == null || opc == null) {
            return descr;
        }

        PortNumber number = descr.portNumber();
        // handle PortNumber "name" portion
        if (!opc.name().isEmpty()) {
            number = PortNumber.portNumber(descr.portNumber().toLong(), opc.name());
        }

        // handle additional annotations
        SparseAnnotations annotations = combine(opc, descr.annotations());

        // (Future work) handle type overwrite?
        Type type = firstNonNull(opc.type(), descr.type());
        if (type != descr.type()) {
            // TODO: Do we need to be able to overwrite Port.Type?
            log.warn("Port type overwrite requested for {}. Ignoring.", cp);
        }

        return updateDescription(number, annotations, descr);
    }

    @Override
    public PortDescription combine(ConnectPoint cp, PortDescription descr, Optional<Config> prevConf) {
        return combine(cp, descr);
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

        if (port.exactlyEquals(descr.portNumber()) && sa.equals(descr.annotations())) {
            // result is no-op
            return descr;
        }
        return DefaultPortDescription.builder(descr)
                .withPortNumber(port)
                .annotations(sa)
                .build();
    }

    /**
     * Generates an annotation from an existing annotation and OptcalPortConfig.
     *
     * @param opc the port config entity from network config
     * @param an the annotation
     * @return annotation combining both sources
     */
    private static SparseAnnotations combine(OpticalPortConfig opc, SparseAnnotations an) {
        DefaultAnnotations.Builder b = DefaultAnnotations.builder();
        b.putAll(an);
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
        return b.build();
    }


}
