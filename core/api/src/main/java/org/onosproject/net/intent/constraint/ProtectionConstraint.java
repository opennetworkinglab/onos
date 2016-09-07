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

package org.onosproject.net.intent.constraint;

import com.google.common.annotations.Beta;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.ResourceContext;

/**
 * Constraint that determines whether to employ path protection.
 */
@Beta
public class ProtectionConstraint implements Constraint {
    private static final ProtectionConstraint PROTECTION_CONSTRAINT = new ProtectionConstraint();

    // doesn't use LinkResourceService
    @Override
    public double cost(Link link, ResourceContext context) {
        return 1;
    }

    // doesn't use LinkResourceService
    @Override
    public boolean validate(Path path, ResourceContext context) {
        return true;
    }

    /**
     * Determines whether to utilize path protection for the given intent.
     *
     * @param intent  intent to be inspected
     * @return        whether the intent has a ProtectionConstraint
     */
    public static boolean requireProtectedPath(Intent intent) {
        if (intent instanceof PointToPointIntent) {
            PointToPointIntent pointToPointIntent = (PointToPointIntent) intent;
            return pointToPointIntent.constraints().stream()
                    .anyMatch(p -> p instanceof ProtectionConstraint);
        }
        return false;
    }

    /**
     * Returns protection constraint.
     *
     * @return
     */
    public static ProtectionConstraint protection() {
        return PROTECTION_CONSTRAINT;
    }

    protected  ProtectionConstraint() {
    }

    @Override
    public String toString() {
        return "Protection";
    }
}
