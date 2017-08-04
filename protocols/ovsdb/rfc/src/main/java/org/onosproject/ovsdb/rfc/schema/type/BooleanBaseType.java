/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.ovsdb.rfc.schema.type;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * One of the strings "integer", "real", "boolean", "string", or "uuid",
 * representing the specified scalar type. Refer to RFC 7047 Section 3.2.
 * Because BooleanBaseType has no constraint conditions, and in order to be
 * consistent with other BaseType, so this class is empty except for the
 * toString method.
 */
public final class BooleanBaseType implements BaseType {

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}
