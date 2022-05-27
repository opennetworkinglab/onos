/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.codec;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroup;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupRule;

/**
 * Hamcrest matcher for security group.
 */
public final class KubevirtSecurityGroupJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String RULES = "rules";

    private final KubevirtSecurityGroup sg;

    private KubevirtSecurityGroupJsonMatcher(KubevirtSecurityGroup sg) {
        this.sg = sg;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {
        // check sg ID
        String jsonId = jsonNode.get(ID).asText();
        String id = sg.id();
        if (!jsonId.equals(id)) {
            description.appendText("ID was " + jsonId);
            return false;
        }

        // check sg name
        String jsonName = jsonNode.get(NAME).asText();
        String name = sg.name();
        if (!jsonName.equals(name)) {
            description.appendText("Name was " + jsonName);
            return false;
        }

        // check description
        JsonNode jsonDescription = jsonNode.get(DESCRIPTION);
        if (jsonDescription != null) {
            String myDescription = sg.description();
            if (!jsonDescription.asText().equals(myDescription)) {
                description.appendText("Description was " + jsonDescription);
                return false;
            }
        }

        JsonNode jsonSgr = jsonNode.get(RULES);
        if (jsonSgr != null) {
            // check size of rule array
            if (jsonSgr.size() != sg.rules().size()) {
                description.appendText("Rules was " + jsonSgr.size());
                return false;
            }

            // check rules
            for (KubevirtSecurityGroupRule sgr : sg.rules()) {
                boolean ruleFound = false;
                for (int ruleIndex = 0; ruleIndex < jsonSgr.size(); ruleIndex++) {
                    KubevirtSecurityGroupRuleJsonMatcher ruleMatcher =
                            KubevirtSecurityGroupRuleJsonMatcher
                                    .matchesKubevirtSecurityGroupRule(sgr);
                    if (ruleMatcher.matches(jsonSgr.get(ruleIndex))) {
                        ruleFound = true;
                        break;
                    }
                }

                if (!ruleFound) {
                    description.appendText("Rule not found " + sgr.toString());
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(sg.toString());
    }

    /**
     * Factory to allocate a kubevirt security group matcher.
     *
     * @param sg kubevirt security group object we are looking for
     * @return matcher
     */
    public static KubevirtSecurityGroupJsonMatcher
    matchesKubevirtSecurityGroup(KubevirtSecurityGroup sg) {
        return new KubevirtSecurityGroupJsonMatcher(sg);
    }
}
