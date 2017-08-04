/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net;

/**
 * Utility for processing annotations.
 */
public final class AnnotationsUtil {

    public static boolean isEqual(Annotations lhs, Annotations rhs) {
        if (lhs == rhs) {
            return true;
        }
        if (lhs == null || rhs == null) {
            return false;
        }

        if (!lhs.keys().equals(rhs.keys())) {
            return false;
        }

        for (String key : lhs.keys()) {
            if (lhs.value(key) == null && rhs.value(key) != null) {
                return false;
            }

            if (lhs.value(key) != null && !lhs.value(key).equals(rhs.value(key))) {
                return false;
            }
        }
        return true;
    }

    // not to be instantiated
    private AnnotationsUtil() {}
}
