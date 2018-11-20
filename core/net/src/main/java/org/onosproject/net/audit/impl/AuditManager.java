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

package org.onosproject.net.audit.impl;

import org.onlab.rest.AuditFilter;

import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cli.AbstractShellCommand;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import java.util.Dictionary;

import static org.onlab.util.Tools.get;
import static org.onosproject.net.OsgiPropertyConstants.AUDIT_FILE_TYPE_DESC;
import static org.onosproject.net.OsgiPropertyConstants.AUDIT_FILE_TYPE_DEFAULT;
import static org.onosproject.net.OsgiPropertyConstants.AUDIT_STATUS_DESC;
import static org.onosproject.net.OsgiPropertyConstants.AUDIT_STATUS_DEFAULT;


/**
 * Component to manage REST API Audit.
 */
@Component(
        immediate = true,
        property = {
                AUDIT_FILE_TYPE_DESC + "=" + AUDIT_FILE_TYPE_DEFAULT,
                AUDIT_STATUS_DESC + ":Boolean=" + AUDIT_STATUS_DEFAULT
        })
public class AuditManager {

    public String auditFile = AUDIT_FILE_TYPE_DEFAULT;
    public boolean auditEnabled = AUDIT_STATUS_DEFAULT;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        setAuditStatus(auditFile, auditEnabled);
    }

    @Modified
    protected void modifyFileType(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        if (properties == null) {
            return;
        }
        auditFile = get(properties, AUDIT_FILE_TYPE_DESC);
        String enableAuditStr = get(properties, AUDIT_STATUS_DESC);

        auditEnabled = Boolean.parseBoolean(enableAuditStr);
        setAuditStatus(auditFile, auditEnabled);
    }

    /**
     * To enable Audit and set file type for REST API and  CLI as  per the changes in configuration properties.
     *
     * @param auditFile    file which audit logs are saved.
     * @param auditEnabled status of REST API Audit and CLI Audit.
     */
    public void setAuditStatus(String auditFile, boolean auditEnabled) {
        if (auditEnabled) {
            AuditFilter.enableAudit();
            AbstractShellCommand.enableAudit();
        } else {
            AuditFilter.disableAudit();
            AbstractShellCommand.disableAudit();
        }
        AuditFilter.setAuditFile(auditFile);
        AbstractShellCommand.setAuditFile(auditFile);
    }
}
