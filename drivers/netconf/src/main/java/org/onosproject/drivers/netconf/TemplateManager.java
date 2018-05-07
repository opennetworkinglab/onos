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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.google.common.io.CharSource;
import com.google.common.io.Resources;

/**
 * Manages templates and provides utilities to execute these templates against a
 * NETCONF session.
 */
public final class TemplateManager {
    private static final Logger log = getLogger(TemplateManager.class);
    private final Map<String, String> templates = new HashMap<>();
    private TemplateRequestDriver requestDriver;
    private static final Map<String, Object> EMPTY_TEMPLATE_CONTEXT = new HashMap<>();

    /**
     * Internal implementation of the request driver that implements a NETCONF
     * driver.
     */
    private class InternalRequestDriver implements TemplateRequestDriver {
        @Override
        public Object doRequest(NetconfSession session, String templateName, Map<String, Object> templateContext,
                String baseXPath, QName returnType) throws NetconfException {
            try {
                String data = session.rpc(render(templates.get(templateName), templateContext)).get();
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document document = builder.parse(new InputSource(CharSource.wrap(data).openStream()));
                XPath xp = XPathFactory.newInstance().newXPath();
                return xp.evaluate(baseXPath, document, returnType);
            } catch (Exception e) {
                throw new NetconfException(e.getMessage(), e);
            }
        }
    }

    /**
     * Constructs a new template manager and loads the default templates.
     */
    public TemplateManager() {
        requestDriver = new InternalRequestDriver();
    }

    /**
     * Sets the request driver for the template manager.
     *
     * @param driver
     *            the driver to use
     */
    public void setRequestDriver(TemplateRequestDriver driver) {
        requestDriver = driver;
    }

    /**
     * Loads the named templates into the template manager.
     *
     * @param reference
     *            the class reference from which to load resources
     * @param pattern
     *            pattern to convert template name to resource path
     * @param templateNames
     *            list of template to load
     */
    public void load(Class<? extends Object> reference, String pattern, String... templateNames) {
        for (String name : templateNames) {
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

            log.debug("LOAD TEMPLATE: '{}' as '{}' from '{}", name, key, resource);

            try {
                templates.put(name,
                        Resources.toString(Resources.getResource(reference, resource), StandardCharsets.UTF_8));

            } catch (IOException ioe) {
                log.error("Unable to load NETCONF request template '{}' from '{}'", key, resource, ioe);
            }
        }
    }

    /**
     * Loads the named templates into the template manager using the default
     * reference class and resource path pattern.
     *
     * @param templateMNames
     *            list of template to load
     */
    public void load(String... templateMNames) {
        load(this.getClass(), "/templates/requests/%s.j2", templateMNames);
    }

    /**
     * Loads the named templates into the template manager using the default
     * reference class.
     *
     * @param pattern
     *            pattern to convert template name to resource path
     * @param templateMNames
     *            list of template to load
     */
    public void load(String pattern, String... templateMNames) {
        load(this.getClass(), pattern, templateMNames);
    }

    /**
     * Returns the named template.
     *
     * @param templateName
     *            name of template to return
     * @return template
     */
    public String get(String templateName) {
        return templates.get(templateName);
    }

    /**
     * Performs simple variable substitution into a string in likely the most
     * inefficient way possible.
     *
     * @param template
     *            template into which to substitute variables
     * @param context
     *            variable substitution map
     * @return template rendered with variable substitution
     */
    public String render(String template, Map<String, Object> context) {
        if (context == null) {
            return template;
        }

        String temp = template;
        for (Map.Entry<String, Object> e : context.entrySet()) {
            temp = temp.replaceAll("\\{\\{ *" + e.getKey() + " *\\}\\}", e.getValue().toString());
        }
        return temp;
    }

    /**
     * Executes the named NETCONF template against the specified session, returning
     * the referenced XML node as the specified type.
     *
     * @param session
     *            NETCONF serssion
     * @param templateName
     *            name of NETCONF request template to execute
     * @param templateContext
     *            variable to values substitutions to be used against templates
     * @param baseXPath
     *            XPath expression to specify the returned document node
     * @param returnType
     *            expected return type of the referenced node
     * @return XML document node referenced by the {@code baseXPath}
     * @throws NetconfException
     *             if any IO, XPath, or NETCONF exception occurs
     */
    public Object doRequest(NetconfSession session, String templateName, Map<String, Object> templateContext,
            String baseXPath, QName returnType) throws NetconfException {
        return requestDriver.doRequest(session, templateName, templateContext, baseXPath, returnType);
    }

    /**
     * Execute the named NETCONF template against the specified session returning
     * the {@code /rpc-reply/data} section of the response document as a
     * {@code Node}.
     *
     * @param session
     *            NETCONF session
     * @param templateName
     *            name of NETCONF request template to execute
     * @return XML document node that represents the NETCONF response data
     * @throws NetconfException
     *             if any IO, XPath, or NETCONF exception occurs
     */
    public Node doRequest(NetconfSession session, String templateName) throws NetconfException {
        return (Node) doRequest(session, templateName, EMPTY_TEMPLATE_CONTEXT, "/rpc-reply/data", XPathConstants.NODE);
    }

    /**
     * Execute the named NETCONF template with the given template context against
     * the specified session returning the {@code /rpc-reply/data} section of the
     * response document as a {@code Node}.
     *
     * @param session
     *            NETCONF session
     * @param templateName
     *            name of NETCONF request template to execute
     * @param templateContext
     *            variables to substitute into the template
     * @return XML document node that represents the NETCONF response data
     * @throws NetconfException
     *             if any IO, XPath, or NETCONF exception occurs
     */
    public Node doRequest(NetconfSession session, String templateName, Map<String, Object> templateContext)
            throws NetconfException {
        return (Node) doRequest(session, templateName, templateContext, "/rpc-reply/data", XPathConstants.NODE);
    }
}
