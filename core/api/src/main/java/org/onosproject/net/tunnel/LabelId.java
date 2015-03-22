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
package org.onosproject.net.tunnel;

import java.util.Objects;
import com.google.common.primitives.UnsignedLongs;

/**
 * Representation of a label Id, a logical port identifier.
 */
public final class LabelId {
        /**
         * Represents a logical Id.
        */
        private final long labelId;

        /**
         * Constructor, public creation is prohibited.
         */
        private LabelId(long id) {
            this.labelId = id;
        }

        /**
         * Returns the LabelId representing the specified long value.
         *
         * @param id identifier as long value
         * @return LabelId
         */
        public static LabelId labelId(long id) {
            return new LabelId(id);
        }

        public static LabelId labelId(String string) {
            return new LabelId(UnsignedLongs.decode(string));
        }

        public long toLong() {
            return labelId;
        }

        @Override
        public String toString() {
            return UnsignedLongs.toString(labelId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(labelId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof LabelId) {
                final LabelId other = (LabelId) obj;
                return this.labelId == other.labelId;
            }
            return false;
        }

}
