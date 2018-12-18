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

import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.security.AuditService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import static org.onlab.util.Tools.get;
import static org.onosproject.net.OsgiPropertyConstants.AUDIT_LOGGER;
import static org.onosproject.net.OsgiPropertyConstants.AUDIT_LOGGER_DEFAULT;
import static org.onosproject.net.OsgiPropertyConstants.AUDIT_ENABLED;
import static org.onosproject.net.OsgiPropertyConstants.AUDIT_ENABLED_DEFAULT;

/**
 * Component to manage audit logging.
 */
@Component(
        immediate = true,
        service = {AuditService.class},
        property = {
                AUDIT_ENABLED + ":Boolean=" + AUDIT_ENABLED_DEFAULT,
                AUDIT_LOGGER + "=" + AUDIT_LOGGER_DEFAULT,
        })
public class AuditManager implements AuditService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Logger auditLog = log;

    /**
     * Specifies whether or not audit logging is enabled.
     */
    private boolean auditEnabled = AUDIT_ENABLED_DEFAULT;

    /**
     * Name of the audit logger.
     */
    private String auditLogger = AUDIT_LOGGER_DEFAULT;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
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
            auditLog.info("user={}; action={}", user, action);
        }
    }

}
