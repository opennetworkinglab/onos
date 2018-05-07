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
package org.onosproject.drivers.netconf;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.onosproject.net.DeviceId;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.w3c.dom.Document;

import com.google.common.io.Resources;

public class MockTemplateRequestDriver implements TemplateRequestDriver {
    private static final Logger log = getLogger(MockTemplateRequestDriver.class);
    private static final DeviceId DEFAULT_RESPONSES_ID = DeviceId.deviceId("mock:default:1234");

    private Map<DeviceId, Map<String, String>> responses = new HashMap<DeviceId, Map<String, String>>();

    private Map<NetconfSession, DeviceId> sessionMap = new HashMap<NetconfSession, DeviceId>();

    @Override
    public Object doRequest(NetconfSession session, String templateName, Map<String, Object> templateContext,
            String baseXPath, QName returnType) throws NetconfException {

        try {
            DeviceId deviceId = sessionMap.get(session);
            Map<String, String> deviceResponses = responses.get(deviceId);
            String responseTemplate = null;
            if (deviceResponses != null) {
                responseTemplate = deviceResponses.get(templateName);
            }
            if (responseTemplate == null) {
                deviceResponses = responses.get(DEFAULT_RESPONSES_ID);
                if (deviceResponses != null) {
                    responseTemplate = deviceResponses.get(templateName);
                }
            }
            if (responseTemplate == null) {
                throw new Exception(
                        String.format("Reponse template '%s' for device '%s' not found", templateName, deviceId));
            }
            InputStream resp = IOUtils.toInputStream(responseTemplate, StandardCharsets.UTF_8);
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document document = builder.parse(resp);
            XPath xp = XPathFactory.newInstance().newXPath();
            return xp.evaluate(baseXPath, document, returnType);
        } catch (Exception e) {
            NetconfException ne = new NetconfException(e.getMessage(), e);
            throw ne;
        }
    }

    public void load(Class<? extends Object> reference, String pattern, DeviceId id, String... reponseNames) {
        for (String name : reponseNames) {
            String key = name;
            String resource;

            // If the template name begins with a '/', then assume it is a full path
            // specification
            if (name.charAt(0) == '/') {
                int start = name.lastIndexOf('/') + 1;
                int end = name.lastIndexOf('.');
                if (end == -1) {
                    key = name.substring(start);
                } else {
                    key = name.substring(start, end);
                }
                resource = name;
            } else {
                resource = String.format(pattern, name);
            }

            log.debug("LOAD RESPONSE TEMPLATE: '{}' as '{}' from '{}'", name, key, resource);

            try {
                DeviceId use = id;
                if (use == null) {
                    use = DEFAULT_RESPONSES_ID;
                }
                Map<String, String> deviceResponses = responses.get(use);
                if (deviceResponses == null) {
                    deviceResponses = new HashMap<String, String>();
                    responses.put(use, deviceResponses);
                }
                deviceResponses.put(name,
                        Resources.toString(Resources.getResource(reference, resource), StandardCharsets.UTF_8));
            } catch (IOException ioe) {
                log.error("Unable to load NETCONF response template '{}' from '{}'", key, resource, ioe);
            }
        }
    }

    public void setDeviceMap(Map<DeviceId, NetconfDevice> devicesMap) {
        // sessionMap.clear();

        for (Map.Entry<DeviceId, NetconfDevice> entry : devicesMap.entrySet()) {
            sessionMap.put(entry.getValue().getSession(), entry.getKey());
        }
    }
}
