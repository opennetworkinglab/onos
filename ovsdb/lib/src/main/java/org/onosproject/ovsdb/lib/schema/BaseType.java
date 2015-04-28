/*
 * Copyright (C) 2014 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran
 */
package org.onosproject.ovsdb.lib.schema;

import java.util.Set;

import org.onosproject.ovsdb.lib.error.TyperException;
import org.onosproject.ovsdb.lib.notation.ReferencedRow;
import org.onosproject.ovsdb.lib.notation.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;

public abstract class BaseType<E extends BaseType<E>> {

    private static BaseType[] types = new BaseType[] {new StringBaseType(),
                                                      new IntegerBaseType(),
                                                      new RealBaseType(),
                                                      new BooleanBaseType(),
                                                      new UuidBaseType(), };

    public static BaseType fromJson(JsonNode json, String keyorval) {
        BaseType baseType = null;
        if (json.isValueNode()) {
            for (BaseType baseTypeFactory : types) {
                String type = json.asText().trim();
                baseType = baseTypeFactory.fromString(type);
                if (baseType != null) {
                    break;
                }
            }
        } else {
            if (!json.has(keyorval)) {
                throw new TyperException("Not a type");
            }

            for (BaseType baseTypeFactory : types) {
                baseType = baseTypeFactory.fromJsonNode(json.get(keyorval),
                                                        keyorval);
                if (baseType != null) {
                    break;
                }
            }
        }
        return baseType;
    }

    protected abstract E fromString(String type);

    protected abstract void getConstraints(E baseType, JsonNode type);

    protected E fromJsonNode(JsonNode type, String keyorval) {

        E baseType = null;

        // json like "string"
        if (type.isTextual()) {
            baseType = fromString(type.asText());
            if (baseType != null) {
                return baseType;
            }
        }

        // json like {"type" : "string", "enum": ["set", ["access",
        // "native-tagged"]]}" for key or value
        if (type.isObject() && type.has("type")) {
            baseType = fromString(type.get("type").asText());
            if (baseType != null) {
                getConstraints(baseType, type);
            }
        }

        return baseType;
    }

    public abstract Object toValue(JsonNode value);

    public abstract void validate(Object value);

    public static class IntegerBaseType extends BaseType<IntegerBaseType> {
        long min = Long.MIN_VALUE;
        long max = Long.MAX_VALUE;
        Set<Integer> enums;

        @Override
        public IntegerBaseType fromString(String typeString) {
            return "integer".equals(typeString) ? new IntegerBaseType() : null;
        }

        @Override
        protected void getConstraints(IntegerBaseType baseType, JsonNode type) {

            JsonNode node = null;
            node = type.get("maxInteger");
            if (node != null) {
                baseType.setMax(node.asLong());
            }
            node = type.get("minInteger");
            if (node != null) {
                baseType.setMin(node.asLong());
            }

            populateEnum(type);
        }

        @Override
        public Object toValue(JsonNode value) {
            return value.asLong();
        }

        @Override
        public void validate(Object value) {

        }

        private void populateEnum(JsonNode node) {
            if (node.has("enum")) {
                Set<Long> s = Sets.newHashSet();
                JsonNode anEnum = node.get("enum").get(1);
                for (JsonNode n : anEnum) {
                    s.add(n.asLong());
                }
            }
        }

        public long getMin() {
            return min;
        }

        public void setMin(long min) {
            this.min = min;
        }

        public long getMax() {
            return max;
        }

        public void setMax(long max) {
            this.max = max;
        }

        public Set<Integer> getEnums() {
            return enums;
        }

        public void setEnums(Set<Integer> enums) {
            this.enums = enums;
        }

        @Override
        public String toString() {
            return "IntegerBaseType";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((enums == null) ? 0 : enums.hashCode());
            result = prime * result + (int) (max ^ (max >>> 32));
            result = prime * result + (int) (min ^ (min >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            IntegerBaseType other = (IntegerBaseType) obj;
            if (enums == null) {
                if (other.enums != null) {
                    return false;
                }
            } else if (!enums.equals(other.enums)) {
                return false;
            }
            if (max != other.max) {
                return false;
            }
            if (min != other.min) {
                return false;
            }
            return true;
        }
    }

    public static class RealBaseType extends BaseType<RealBaseType> {
        double min = Double.MIN_VALUE;
        double max = Double.MAX_VALUE;
        Set<Double> enums;

        @Override
        public RealBaseType fromString(String typeString) {
            return "real".equals(typeString) ? new RealBaseType() : null;
        }

        @Override
        protected void getConstraints(RealBaseType baseType, JsonNode type) {

            JsonNode node = null;
            node = type.get("maxReal");
            if (node != null) {
                baseType.setMax(node.asLong());
            }
            node = type.get("minReal");
            if (node != null) {
                baseType.setMin(node.asLong());
            }

            populateEnum(type);
        }

        @Override
        public Object toValue(JsonNode value) {
            return value.asDouble();
        }

        @Override
        public void validate(Object value) {

        }

        private void populateEnum(JsonNode node) {
            if (node.has("enum")) {
                Set<Double> s = Sets.newHashSet();
                JsonNode anEnum = node.get("enum").get(1);
                for (JsonNode n : anEnum) {
                    s.add(n.asDouble());
                }
            }
        }

        public double getMin() {
            return min;
        }

        public void setMin(double min) {
            this.min = min;
        }

        public double getMax() {
            return max;
        }

        public void setMax(double max) {
            this.max = max;
        }

        public Set<Double> getEnums() {
            return enums;
        }

        public void setEnums(Set<Double> enums) {
            this.enums = enums;
        }

        @Override
        public String toString() {
            return "RealBaseType";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((enums == null) ? 0 : enums.hashCode());
            long temp;
            temp = Double.doubleToLongBits(max);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(min);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            RealBaseType other = (RealBaseType) obj;
            if (enums == null) {
                if (other.enums != null) {
                    return false;
                }
            } else if (!enums.equals(other.enums)) {
                return false;
            }
            if (Double.doubleToLongBits(max) != Double
                    .doubleToLongBits(other.max)) {
                return false;
            }
            if (Double.doubleToLongBits(min) != Double
                    .doubleToLongBits(other.min)) {
                return false;
            }
            return true;
        }
    }

    public static class BooleanBaseType extends BaseType {

        @Override
        public BooleanBaseType fromString(String typeString) {
            return "boolean".equals(typeString) ? new BooleanBaseType() : null;
        }

        @Override
        protected void getConstraints(BaseType baseType, JsonNode node) {
            // no op
        }

        @Override
        public Object toValue(JsonNode value) {
            return value.asBoolean();
        }

        @Override
        public void validate(Object value) {

        }

        @Override
        public String toString() {
            return "BooleanBaseType";
        }
    }

    public static class StringBaseType extends BaseType<StringBaseType> {
        int minLength = Integer.MIN_VALUE;
        int maxLength = Integer.MAX_VALUE;
        Set<String> enums;

        @Override
        public StringBaseType fromString(String typeString) {
            return "string".equals(typeString) ? new StringBaseType() : null;
        }

        @Override
        protected void getConstraints(StringBaseType baseType, JsonNode type) {

            JsonNode node = null;
            node = type.get("maxLength");
            if (node != null) {
                baseType.setMaxLength(node.asInt());
            }
            node = type.get("minLength");
            if (node != null) {
                baseType.setMinLength(node.asInt());
            }

            populateEnum(baseType, type);
        }

        @Override
        public Object toValue(JsonNode value) {
            return value.asText();
        }

        @Override
        public void validate(Object value) {

        }

        private void populateEnum(StringBaseType baseType, JsonNode node) {
            if (node.has("enum")) {
                Set<String> s = Sets.newHashSet();
                JsonNode enumVal = node.get("enum");
                if (enumVal.isArray()) {
                    JsonNode anEnum = enumVal.get(1);
                    for (JsonNode n : anEnum) {
                        s.add(n.asText());
                    }
                } else if (enumVal.isTextual()) {
                    s.add(enumVal.asText());
                }
                baseType.setEnums(s);
            }
        }

        public int getMinLength() {
            return minLength;
        }

        public void setMinLength(int minLength) {
            this.minLength = minLength;
        }

        public int getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }

        public Set<String> getEnums() {
            return enums;
        }

        public void setEnums(Set<String> enums) {
            this.enums = enums;
        }

        @Override
        public String toString() {
            return "StringBaseType";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((enums == null) ? 0 : enums.hashCode());
            result = prime * result + maxLength;
            result = prime * result + minLength;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            StringBaseType other = (StringBaseType) obj;
            if (enums == null) {
                if (other.enums != null) {
                    return false;
                }
            } else if (!enums.equals(other.enums)) {
                return false;
            }
            if (maxLength != other.maxLength) {
                return false;
            }
            if (minLength != other.minLength) {
                return false;
            }
            return true;
        }

    }

    public static class UuidBaseType extends BaseType<UuidBaseType> {
        public static enum RefType {
            strong, weak
        }

        String refTable;
        RefType refType;

        @Override
        public UuidBaseType fromString(String typeString) {
            return "uuid".equals(typeString) ? new UuidBaseType() : null;
        }

        @Override
        protected void getConstraints(UuidBaseType baseType, JsonNode node) {

            JsonNode refTable = node.get("refTable");
            baseType.setRefTable(refTable != null ? refTable.asText() : null);

            JsonNode refTypeJson = node.get("refType");
            baseType.setRefType(refTypeJson != null ? RefType
                    .valueOf(refTypeJson.asText()) : RefType.strong);

        }

        @Override
        public Object toValue(JsonNode value) {
            if (value.isArray()) {
                if (value.size() == 2) {
                    if (value.get(0).isTextual()
                            && "uuid".equals(value.get(0).asText())) {
                        return new UUID(value.get(1).asText());
                    }
                }
            } else {
                /*
                 * UUIDBaseType used by RefTable from SouthBound will always be
                 * an Array of ["uuid", <uuid>]. But there are some cases from
                 * northbound where the RefTable type can be expanded to a Row
                 * with contents. In those scenarios, just retain the content
                 * and return a ReferencedRow for the upper layer functions to
                 * process it.
                 */
                return new ReferencedRow(refTable, value);
            }
            return null;
        }

        @Override
        public void validate(Object value) {

        }

        public String getRefTable() {
            return refTable;
        }

        public void setRefTable(String refTable) {
            this.refTable = refTable;
        }

        public RefType getRefType() {
            return refType;
        }

        public void setRefType(RefType refType) {
            this.refType = refType;
        }

        @Override
        public String toString() {
            return "UuidBaseType";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((refTable == null) ? 0 : refTable.hashCode());
            result = prime * result
                    + ((refType == null) ? 0 : refType.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            UuidBaseType other = (UuidBaseType) obj;
            if (refTable == null) {
                if (other.refTable != null) {
                    return false;
                }
            } else if (!refTable.equals(other.refTable)) {
                return false;
            }
            if (refType != other.refType) {
                return false;
            }
            return true;
        }
    }
}
