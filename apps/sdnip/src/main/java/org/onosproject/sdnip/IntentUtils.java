/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.sdnip;

import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Utilities for dealing with intents.
 */
public final class IntentUtils {

    private static final Logger log = LoggerFactory.getLogger(IntentUtils.class);

    private IntentUtils() {

    }

    /**
     * Checks if two intents represent the same value.
     *
     * <p>({@link Intent#equals(Object)} only checks ID equality)</p>
     *
     * <p>Both intents must be of the same type.</p>
     *
     * @param one first intent
     * @param two second intent
     * @return true if the two intents represent the same value, otherwise false
     */
    public static boolean equals(Intent one, Intent two) {
        checkArgument(one.getClass() == two.getClass(),
                "Intents are not the same type");

        if (!(Objects.equals(one.appId(), two.appId()) &&
                Objects.equals(one.key(), two.key()))) {
            return false;
        }

        if (one instanceof MultiPointToSinglePointIntent) {
            MultiPointToSinglePointIntent intent1 = (MultiPointToSinglePointIntent) one;
            MultiPointToSinglePointIntent intent2 = (MultiPointToSinglePointIntent) two;

            return Objects.equals(intent1.selector(), intent2.selector()) &&
                    Objects.equals(intent1.treatment(), intent2.treatment()) &&
                    Objects.equals(intent1.ingressPoints(), intent2.ingressPoints()) &&
                    Objects.equals(intent1.egressPoint(), intent2.egressPoint());
        } else if (one instanceof PointToPointIntent) {
            PointToPointIntent intent1 = (PointToPointIntent) one;
            PointToPointIntent intent2 = (PointToPointIntent) two;

            return Objects.equals(intent1.selector(), intent2.selector()) &&
                    Objects.equals(intent1.treatment(), intent2.treatment()) &&
                    Objects.equals(intent1.ingressPoint(), intent2.ingressPoint()) &&
                    Objects.equals(intent1.egressPoint(), intent2.egressPoint());
        } else {
            log.error("Unimplemented intent type");
            return false;
        }
    }
}
