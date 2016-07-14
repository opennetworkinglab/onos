/*
 * Copyright 2015-present Open Networking Laboratory
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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.onosproject.ovsdb.rfc.notation.Row;
import org.onosproject.ovsdb.rfc.notation.Uuid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * All results of ovs table operations. refer to RFC7047 5.2.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class OperationResult {
    private int count;
    private Uuid uuid;
    private List<Row> rows;
    private String error;
    private String details;

    /**
     * Constructs a OperationResult object. When JsonNode is converted into
     * OperationResult, need this constructor and also need setter method.
     */
    public OperationResult() {
    }

    /**
     * Constructs a OperationResult object.
     * @param rows List of Row entity
     */
    public OperationResult(List<Row> rows) {
        checkNotNull(rows, "rows cannot be null");
        this.rows = rows;
    }

    /**
     * Constructs a OperationResult object.
     * @param count the count node of result
     * @param uuid UUID entity
     * @param rows List of Row entity
     * @param error error message
     * @param details details of error message
     */
    public OperationResult(int count, Uuid uuid, List<Row> rows, String error,
                           String details) {
        checkNotNull(uuid, "uuid cannot be null");
        checkNotNull(rows, "rows cannot be null");
        checkNotNull(error, "error cannot be null");
        checkNotNull(details, "details cannot be null");
        this.count = count;
        this.uuid = uuid;
        this.rows = rows;
        this.error = error;
        this.details = details;
    }

    /**
     * Return count.
     * @return count
     */
    public int getCount() {
        return count;
    }

    /**
     * Set count value.
     * @param count the Operation message of count
     */
    public void setCount(int count) {
        this.count = count;
    }

    /**
     * Return uuid.
     * @return uuid
     */
    public Uuid getUuid() {
        return uuid;
    }

    /**
     * Set uuid value.
     * @param uuid the Operation message of uuid
     */
    public void setUuid(Uuid uuid) {
        checkNotNull(uuid, "uuid cannot be null");
        this.uuid = uuid;
    }

    /**
     * Return rows.
     * @return List of Row
     */
    public List<Row> getRows() {
        return rows;
    }

    /**
     * Set rows value.
     * @param rows the Operation message of rows
     */
    public void setRows(List<Row> rows) {
        checkNotNull(rows, "rows cannot be null");
        this.rows = rows;
    }

    /**
     * Return error.
     * @return error
     */
    public String getError() {
        return error;
    }

    /**
     * Set error value.
     * @param error the Operation message of error
     */
    public void setError(String error) {
        checkNotNull(error, "error cannot be null");
        this.error = error;
    }

    /**
     * Return details.
     * @return details
     */
    public String getDetails() {
        return details;
    }

    /**
     * Set details value.
     * @param details the Operation message of details
     */
    public void setDetails(String details) {
        checkNotNull(details, "details cannot be null");
        this.details = details;
    }
}
