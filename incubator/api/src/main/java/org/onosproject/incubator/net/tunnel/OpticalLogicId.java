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

package org.onosproject.incubator.net.tunnel;

import java.util.Objects;

import com.google.common.annotations.Beta;
import com.google.common.primitives.UnsignedLongs;

/**
 * Representation of a label Id, a logical port identifier.
 */
@Beta
public final class OpticalLogicId {
        /**
         * Represents a logical Id.
        */
        private final long logicId;

        /**
         * Constructor, public creation is prohibited.
         */
        private OpticalLogicId(long id) {
            this.logicId = id;
        }

        /**
         * Returns the LabelId representing the specified long value.
         *
         * @param id identifier as long value
         * @return LabelId
         */
        public static OpticalLogicId logicId(long id) {
            return new OpticalLogicId(id);
        }

        public static OpticalLogicId logicId(String string) {
            return new OpticalLogicId(UnsignedLongs.decode(string));
        }

        public long toLong() {
            return logicId;
        }

        @Override
        public String toString() {
            return UnsignedLongs.toString(logicId);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(logicId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof OpticalLogicId) {
                final OpticalLogicId other = (OpticalLogicId) obj;
                return this.logicId == other.logicId;
            }
            return false;
        }

}
