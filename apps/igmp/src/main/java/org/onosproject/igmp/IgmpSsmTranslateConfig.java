/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onosproject.igmp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;
import org.onosproject.net.mcast.McastRoute;

import java.util.ArrayList;
import java.util.List;

/**
 * IGMP SSM translate configuration.
 */
public class IgmpSsmTranslateConfig extends Config<ApplicationId> {

    private static final String SOURCE = "source";
    private static final String GROUP = "group";

    @Override
    public boolean isValid() {
        for (JsonNode node : array) {
            if (!hasOnlyFields((ObjectNode) node, SOURCE, GROUP)) {
                return false;
            }

            if (!(isIpAddress((ObjectNode) node, SOURCE, FieldPresence.MANDATORY) &&
                    isIpAddress((ObjectNode) node, GROUP, FieldPresence.MANDATORY))) {
                return false;
            }

        }
        return true;
    }

    /**
     * Gets the list of SSM translations.
     *
     * @return SSM translations
     */
    public List<McastRoute> getSsmTranslations() {
        List<McastRoute> translations = new ArrayList();
        for (JsonNode node : array) {
            translations.add(
                    new McastRoute(
                            IpAddress.valueOf(node.path(SOURCE).asText().trim()),
                            IpAddress.valueOf(node.path(GROUP).asText().trim()),
                            McastRoute.Type.STATIC));
        }

        return translations;
    }
}
