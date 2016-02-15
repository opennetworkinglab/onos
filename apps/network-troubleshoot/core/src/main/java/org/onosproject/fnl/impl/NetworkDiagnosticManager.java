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
package org.onosproject.fnl.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.fnl.intf.NetworkAnomaly;
import org.onosproject.fnl.intf.NetworkDiagnostic;
import org.onosproject.fnl.intf.NetworkDiagnostic.Type;
import org.onosproject.fnl.intf.NetworkDiagnosticService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of the Network Troubleshooting Core Service.
 *
 * It is simply modularized at present.
 */
@Component(immediate = true)
@Service
public class NetworkDiagnosticManager implements NetworkDiagnosticService {

    /**
     * Name of Network Troubleshooting Application.
     */
    public static final String NTS_APP_NAME =
            "org.onosproject.FNL.Network-Troubleshoot";

    private static final String PROPERTY_AUTO_REGISTER_DIAG =
            "autoRegisterDefaultDiagnostics";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;


    // ------ service below is for auto register ------

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService ds;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hs;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService frs;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService ls;


    @Property(name = PROPERTY_AUTO_REGISTER_DIAG, boolValue = true,
            label = "Automatically register all of default diagnostic modules.")
    private boolean autoRegister = true;


    private ApplicationId appId;

    private Map<Type, NetworkDiagnostic> diagnostics = new ConcurrentHashMap<>();


    @Activate
    protected void activate(ComponentContext context) {
        appId = coreService.registerApplication(NTS_APP_NAME);

        cfgService.registerProperties(getClass());
        readConfiguration(context);

        autoRegisterDiagnostics();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        unregisterDiagnostics();
        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {

        readConfiguration(context);

        // We should not register default diagnostics automatically
        // in the Modify(), because that will erase the result of
        // dynamic extension, run-time and custom diagnostics.
        //
        // And, using this modified() is to avoid deactivate-activate procedure.

        log.info("Modified");
    }

    private void readConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties =  context.getProperties();

        Boolean autoRegisterEnabled =
                Tools.isPropertyEnabled(properties, PROPERTY_AUTO_REGISTER_DIAG);
        if (autoRegisterEnabled == null) {
            log.warn("Auto Register is not configured, " +
                    "using current value of {}", autoRegister);
        } else {
            autoRegister = autoRegisterEnabled;
            log.info("Configured. Auto Register is {}",
                    autoRegister ? "enabled" : "disabled");
        }
    }

    private void autoRegisterDiagnostics() {
        if (!autoRegister) {
            return;
        }

        // TODO: 10/26/16 future new default diagnostics should be added here.
        register(new DefaultCheckLoop(ds, hs, frs, ls));
    }

    private void unregisterDiagnostics() {
        // TODO: 10/27/16 improve this for parallel.
        diagnostics.clear();
    }

    @Override
    public Set<NetworkAnomaly> findAnomalies() {
        Set<NetworkAnomaly> allAnomalies = new HashSet<>();

        diagnostics.forEach((type, diag) ->
                allAnomalies.addAll(diag.findAnomalies()));

        return allAnomalies;
    }

    @Override
    public Set<NetworkAnomaly> findAnomalies(Type type) {
        checkNotNull(type, "NetworkAnomaly Type cannot be null");

        Set<NetworkAnomaly> anomalies = new HashSet<>();

        NetworkDiagnostic diag = diagnostics.get(type);
        if (diag == null) {
            log.warn("no anomalies of type {} found", type);
            return anomalies;
        }

        anomalies.addAll(diag.findAnomalies());

        return anomalies;
    }

    @Override
    public void register(NetworkDiagnostic diag) {
        checkNotNull(diag, "Diagnostic cannot be null");

        NetworkDiagnostic oldDiag = diagnostics.put(diag.type(), diag);

        if (oldDiag != null) {
            log.warn("previous diagnostic {} is replaced by {},",
                    oldDiag.getClass(), diag.getClass());
        } else {
            log.info("Register {} type module: {}",
                    diag.type(), diag.getClass().getName());
        }
    }

    @Override
    public boolean unregister(NetworkDiagnostic diag) {
        checkNotNull(diag, "Diagnostic cannot be null");

        return diagnostics.remove(diag.type(), diag);
    }
}
