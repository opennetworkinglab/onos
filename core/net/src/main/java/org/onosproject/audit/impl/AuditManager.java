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

package org.onosproject.audit.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.security.AuditService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import static org.onlab.util.Tools.get;

/**
 * Component to manage audit logging.
 */
@Component(immediate = true)
@Service
public class AuditManager implements AuditService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Logger auditLog = log;

    private static final String AUDIT_ENABLED = "auditEnabled";
    private static final boolean AUDIT_ENABLED_DEFAULT = false;

    private static final String AUDIT_LOGGER = "auditLogger";
    private static final String AUDIT_LOGGER_DEFAULT = "securityAudit";


    @Property(name = AUDIT_ENABLED, boolValue = AUDIT_ENABLED_DEFAULT,
            label = "Specifies whether or not audit logging is enabled.")
    private boolean auditEnabled = AUDIT_ENABLED_DEFAULT;

    @Property(name = AUDIT_LOGGER, value = AUDIT_LOGGER_DEFAULT,
            label = "Name of the audit logger.")
    private String auditLogger = AUDIT_LOGGER_DEFAULT;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Activate
    protected void activate(ComponentContext ctx) {
        cfgService.registerProperties(getClass());
        modified(ctx);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext ctx) {
        Dictionary<?, ?> properties = ctx.getProperties();
        if (properties != null) {
            auditEnabled = Boolean.parseBoolean(get(properties, AUDIT_ENABLED));
            auditLogger = get(properties, AUDIT_LOGGER);
            auditLog = LoggerFactory.getLogger(auditLogger);
            log.info("Reconfigured; auditEnabled={}; auditLogger={}", auditEnabled, auditLogger);
        }
    }

    @Override
    public boolean isAuditing() {
        return auditEnabled;
    }

    @Override
    public void logUserAction(String user, String action) {
        if (auditEnabled) {
            action = action.concat(" | " + auditLogger);
            auditLog.info("user={}; action={}", user, action);
        }
    }

}
