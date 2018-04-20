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

import java.util.Map;

import javax.xml.namespace.QName;

import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;

public interface TemplateRequestDriver {

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
            String baseXPath, QName returnType) throws NetconfException;
}
