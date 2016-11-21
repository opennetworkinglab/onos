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
package org.onosproject.lisp.ctl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.CoreService;
import org.onosproject.lisp.LispController;
import org.onosproject.lisp.msg.authentication.LispAuthenticationConfig;
import org.onosproject.net.device.DeviceService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.getIntegerProperty;

/**
 * LISP controller initiation class.
 */
@Component(immediate = true)
@Service
public class LispControllerImpl implements LispController {

    private static final String APP_ID = "org.onosproject.lisp-base";

    private static final Logger log =
            LoggerFactory.getLogger(LispControllerImpl.class);

    private static final String DEFAULT_LISP_AUTH_KEY = "onos";
    private static final short DEFAULT_LISP_AUTH_KEY_ID = 1;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Property(name = "lispAuthKey", value = DEFAULT_LISP_AUTH_KEY,
            label = "Authentication key which is used to calculate authentication " +
                    "data for LISP control message; default value is onos")
    protected String lispAuthKey = DEFAULT_LISP_AUTH_KEY;

    @Property(name = "lispAuthKeyId", intValue = DEFAULT_LISP_AUTH_KEY_ID,
            label = "Authentication key id which denotes the authentication method " +
                    "that ONOS uses to calculate the authentication data; " +
                    "1 denotes HMAC SHA1 encryption, 2 denotes HMAC SHA256 encryption; " +
                    "default value is 1")
    protected int lispAuthKeyId = DEFAULT_LISP_AUTH_KEY_ID;

    private final LispControllerBootstrap bootstrap = new LispControllerBootstrap();
    private final LispAuthenticationConfig authConfig = LispAuthenticationConfig.getInstance();

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        coreService.registerApplication(APP_ID);
        initAuthConfig(context.getProperties());
        bootstrap.start();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        bootstrap.stop();
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        readComponentConfiguration(context);
    }

    /**
     * Initializes authentication key and authentication method.
     *
     * @param properties a set of properties that contained in component context
     */
    private void initAuthConfig(Dictionary<?, ?> properties) {
        authConfig.updateLispAuthKey(get(properties, "lispAuthKey"));
        authConfig.updateLispAuthKeyId(getIntegerProperty(properties, "lispAuthKeyId"));
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        String lispAuthKeyStr = Tools.get(properties, "lispAuthKey");
        lispAuthKey = lispAuthKeyStr != null ? lispAuthKeyStr : DEFAULT_LISP_AUTH_KEY;
        authConfig.updateLispAuthKey(lispAuthKey);
        log.info("Configured. LISP authentication key is {}", lispAuthKey);

        Integer lispAuthMethodInt = Tools.getIntegerProperty(properties, "lispAuthKeyId");
        if (lispAuthMethodInt == null) {
            lispAuthKeyId = DEFAULT_LISP_AUTH_KEY_ID;
            log.info("LISP authentication method is not configured, default value is {}", lispAuthKeyId);
        } else {
            lispAuthKeyId = lispAuthMethodInt;
            log.info("Configured. LISP authentication method is configured to {}", lispAuthKeyId);
        }
        authConfig.updateLispAuthKeyId(lispAuthKeyId);
    }
}
