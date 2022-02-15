/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.gnmi.ctl;

import io.grpc.ManagedChannel;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.gnmi.api.GnmiClient;
import org.onosproject.gnmi.api.GnmiController;
import org.onosproject.gnmi.api.GnmiEvent;
import org.onosproject.gnmi.api.GnmiEventListener;
import org.onosproject.grpc.ctl.AbstractGrpcClientController;
import org.onosproject.net.DeviceId;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Dictionary;

import static org.onosproject.gnmi.ctl.OsgiPropertyConstants.READ_PORT_ID;
import static org.onosproject.gnmi.ctl.OsgiPropertyConstants.READ_PORT_ID_DEFAULT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of gNMI controller.
 */
@Component(immediate = true,
        service = GnmiController.class,
        property = {
                READ_PORT_ID + ":Boolean=" + READ_PORT_ID_DEFAULT,
        })
public class GnmiControllerImpl
        extends AbstractGrpcClientController
        <GnmiClient, GnmiEvent, GnmiEventListener>
        implements GnmiController {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private ComponentConfigService componentConfigService;

    /**
     * Configure read port-id for gnmi drivers; default is false.
     */
    private boolean readPortId = READ_PORT_ID_DEFAULT;

    public GnmiControllerImpl() {
        super(GnmiEvent.class, "gNMI");
    }

    @Activate
    public void activate(ComponentContext context) {
        super.activate();
        componentConfigService.registerProperties(getClass());
        modified(context);
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context == null) {
            return;
        }

        Dictionary<?, ?> properties = context.getProperties();
        String strReadPortId = Tools.get(properties, READ_PORT_ID);
        // FIXME temporary solution will be substituted by
        //  an XML driver property when the transition to
        //  p4rt translation is completed
        readPortId = Boolean.parseBoolean(strReadPortId);
        log.info("Configured. {} is configured to {}",
                READ_PORT_ID, readPortId);
    }

    @Override
    protected GnmiClient createClientInstance(
            DeviceId deviceId, ManagedChannel channel) {
        return new GnmiClientImpl(deviceId, channel, this);
    }

    /**
     * Returns whether or not readPortId is enabled.
     *
     * @return true if readPortId is enabled, false otherwise.
     */
    public boolean readPortId() {
        return readPortId;
    }
}
