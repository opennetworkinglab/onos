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
package org.onosproject.ovsdb.rfc.table;

import java.util.Map;

import org.onosproject.ovsdb.rfc.notation.Column;
import org.onosproject.ovsdb.rfc.notation.Row;
import org.onosproject.ovsdb.rfc.schema.DatabaseSchema;
import org.onosproject.ovsdb.rfc.tableservice.AbstractOvsdbTableService;
import org.onosproject.ovsdb.rfc.tableservice.ColumnDescription;

/**
 * This class provides operations of Ssl Table.
 */
public class Ssl extends AbstractOvsdbTableService {
    /**
     * Ssl table column name.
     */
    public enum SslColumn {
        CACERT("ca_cert"), EXTERNALIDS("external_ids"), BOOTSTRAPCACERT("bootstrap_ca_cert"),
        CERTIFICATE("certificate"), PRIVATEKEY("private_key");

        private final String columnName;

        private SslColumn(String columnName) {
            this.columnName = columnName;
        }

        /**
         * Returns the table column name for SslColumn.
         * @return the table column name
         */
        public String columnName() {
            return columnName;
        }
    }

    /**
     * Constructs a Ssl object. Generate Ssl Table Description.
     * @param dbSchema DatabaseSchema
     * @param row Row
     */
    public Ssl(DatabaseSchema dbSchema, Row row) {
        super(dbSchema, row, OvsdbTable.SSL, VersionNum.VERSION100);
    }

    /**
     * Get the Column entity which column name is "ca_cert" from the Row entity
     * of attributes.
     * @return the Column entity
     */
    public Column getCaCertColumn() {
        ColumnDescription columndesc = new ColumnDescription(SslColumn.CACERT.columnName(),
                                                             "getCaCertColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "ca_cert" to the Row entity of
     * attributes.
     * @param caCert the column data which column name is "ca_cert"
     */
    public void setCaCert(String caCert) {
        ColumnDescription columndesc = new ColumnDescription(SslColumn.CACERT.columnName(), "setCaCert",
                                                             VersionNum.VERSION100);
        super.setDataHandler(columndesc, caCert);
    }

    /**
     * Get the Column entity which column name is "external_ids" from the Row
     * entity of attributes.
     * @return the Column entity
     */
    public Column getExternalIdsColumn() {
        ColumnDescription columndesc = new ColumnDescription(SslColumn.EXTERNALIDS.columnName(),
                                                             "getExternalIdsColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "external_ids" to the Row entity
     * of attributes.
     * @param externalIds the column data which column name is "external_ids"
     */
    public void setExternalIds(Map<String, String> externalIds) {
        ColumnDescription columndesc = new ColumnDescription(SslColumn.EXTERNALIDS.columnName(),
                                                             "setExternalIds", VersionNum.VERSION100);
        super.setDataHandler(columndesc, externalIds);
    }

    /**
     * Get the Column entity which column name is "bootstrap_ca_cert" from the
     * Row entity of attributes.
     * @return the Column entity
     */
    public Column getBootstrapCaCertColumn() {
        ColumnDescription columndesc = new ColumnDescription(SslColumn.BOOTSTRAPCACERT.columnName(),
                                                             "getBootstrapCaCertColumn",
                                                             VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "bootstrap_ca_cert" to the Row
     * entity of attributes.
     * @param bootstrapCaCert the column data which column name is
     *            "bootstrap_ca_cert"
     */
    public void setBootstrapCaCert(Boolean bootstrapCaCert) {
        ColumnDescription columndesc = new ColumnDescription(SslColumn.BOOTSTRAPCACERT.columnName(),
                                                             "setBootstrapCaCert", VersionNum.VERSION100);
        super.setDataHandler(columndesc, bootstrapCaCert);
    }

    /**
     * Get the Column entity which column name is "certificate" from the Row
     * entity of attributes.
     * @return the Column entity
     */
    public Column getCertificateColumn() {
        ColumnDescription columndesc = new ColumnDescription(SslColumn.CERTIFICATE.columnName(),
                                                             "getCertificateColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "certificate" to the Row entity
     * of attributes.
     * @param certificate the column data which column name is "certificate"
     */
    public void setCertificate(String certificate) {
        ColumnDescription columndesc = new ColumnDescription(SslColumn.CERTIFICATE.columnName(),
                                                             "setCertificate", VersionNum.VERSION100);
        super.setDataHandler(columndesc, certificate);
    }

    /**
     * Get the Column entity which column name is "private_key" from the Row
     * entity of attributes.
     * @return the Column entity
     */
    public Column getPrivateKeyColumn() {
        ColumnDescription columndesc = new ColumnDescription(SslColumn.PRIVATEKEY.columnName(),
                                                             "getPrivateKeyColumn", VersionNum.VERSION100);
        return (Column) super.getColumnHandler(columndesc);
    }

    /**
     * Add a Column entity which column name is "private_key" to the Row entity
     * of attributes.
     * @param privatekey the column data which column name is "private_key"
     */
    public void setPrivateKey(String privatekey) {
        ColumnDescription columndesc = new ColumnDescription(SslColumn.PRIVATEKEY.columnName(),
                                                             "setPrivateKey", VersionNum.VERSION100);
        super.setDataHandler(columndesc, privatekey);
    }
}
