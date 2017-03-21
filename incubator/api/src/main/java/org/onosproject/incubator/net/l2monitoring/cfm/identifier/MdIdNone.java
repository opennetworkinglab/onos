/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.incubator.net.l2monitoring.cfm.identifier;

/**
 * A representation of NONE as an MA identifier.
 */
public class MdIdNone implements MdId {

    @Override
    public String mdName() {
        return null;
    }

    @Override
    public String toString() {
        return "";
    }

    @Override
    public int getNameLength() {
        return 0;
    }

    @Override
    public MdNameType nameType() {
        return MdNameType.NONE;
    }

    public static MdId asMdId() {
        return new MdIdNone();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MdIdNone) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
