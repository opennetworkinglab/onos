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
package org.onosproject.ovsdb.rfc.message;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

/**
 * The contents of this object specify how the columns or table are to be
 * monitored.
 */
public final class MonitorSelect {

    private final boolean initial;
    private final boolean insert;
    private final boolean delete;
    private final boolean modify;

    /**
     * Constructs a MonitorSelect object.
     * @param initial whether monitor the initial action
     * @param insert whether monitor the insert action
     * @param delete whether monitor the delete action
     * @param modify whether monitor the modify action
     */
    public MonitorSelect(boolean initial, boolean insert, boolean delete,
                         boolean modify) {
        this.initial = initial;
        this.insert = insert;
        this.delete = delete;
        this.modify = modify;
    }

    /**
     * Returns initial.
     * @return initial
     */
    public boolean isInitial() {
        return initial;
    }

    /**
     * Returns insert.
     * @return insert
     */
    public boolean isInsert() {
        return insert;
    }

    /**
     * Returns delete.
     * @return delete
     */
    public boolean isDelete() {
        return delete;
    }

    /**
     * Returns modify.
     * @return modify
     */
    public boolean isModify() {
        return modify;
    }

    @Override
    public int hashCode() {
        return Objects.hash(initial, insert, delete, modify);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof MonitorSelect) {
            final MonitorSelect other = (MonitorSelect) obj;
            return Objects.equals(this.initial, other.initial)
                    && Objects.equals(this.insert, other.insert)
                    && Objects.equals(this.delete, other.delete)
                    && Objects.equals(this.modify, other.modify);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("initial", initial)
                .add("insert", insert).add("delete", delete)
                .add("modify", modify).toString();
    }
}
