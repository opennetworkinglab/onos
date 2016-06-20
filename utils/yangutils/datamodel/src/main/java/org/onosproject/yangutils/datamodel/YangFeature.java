/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.yangutils.datamodel;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;

/*
 * Reference RFC 6020.
 *
 * The "feature" statement is used to define a mechanism by which
 * portions of the schema are marked as conditional.  A feature name is
 * defined that can later be referenced using the "if-feature" statement.
 * Schema nodes tagged with a feature are ignored by the device unless
 * the device supports the given feature.  This allows portions of the
 * YANG module to be conditional based on conditions on the device.
 * The model can represent the abilities of the device within the model,
 * giving a richer model that allows for differing device abilities and roles.
 *
 * The argument to the "feature" statement is the name of the new
 * feature, and follows the rules for identifiers.  This name is used by the
 * "if-feature" statement to tie the schema nodes to the feature.
 *
 * The feature's Substatements
 *
 *                +--------------+---------+-------------+------------------+
 *                | substatement | section | cardinality |data model mapping|
 *                +--------------+---------+-------------+------------------+
 *                | description  | 7.19.3  | 0..1        | -string          |
 *                | if-feature   | 7.18.2  | 0..n        | -YangIfFeature   |
 *                | reference    | 7.19.4  | 0..1        | -string          |
 *                | status       | 7.19.2  | 0..1        | -YangStatus      |
 *                +--------------+---------+-------------+------------------+
 */

/**
 * Represents data model node to maintain information defined in YANG feature.
 */
public class YangFeature implements YangCommonInfo, Parsable, YangIfFeatureHolder, Serializable {

    private static final long serialVersionUID = 806201635L;

    /**
     * Name of the feature.
     */
    private String name;

    /**
     * Description of feature.
     */
    private String description;

    /**
     * Reference of the feature.
     */
    private String reference;

    /**
     * Status of feature.
     */
    private YangStatusType statusType;

    /**
     * List of if-feature.
     */
    private List<YangIfFeature> ifFeatureList;

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getReference() {
        return reference;
    }

    @Override
    public void setReference(String reference) {
        this.reference = reference;
    }

    @Override
    public YangStatusType getStatus() {
        return statusType;
    }

    @Override
    public void setStatus(YangStatusType status) {
        this.statusType = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.FEATURE_DATA;
    }

    @Override
    public void validateDataOnEntry() throws DataModelException {
        //TODO : To be implemented
    }

    @Override
    public void validateDataOnExit() throws DataModelException {
        //TODO : To be implemented
    }

    @Override
    public List<YangIfFeature> getIfFeatureList() {
        return ifFeatureList;
    }

    @Override
    public void addIfFeatureList(YangIfFeature ifFeature) {
        if (getIfFeatureList() == null) {
            setIfFeatureList(new LinkedList<>());
        }
        getIfFeatureList().add(ifFeature);
    }

    @Override
    public void setIfFeatureList(List<YangIfFeature> ifFeatureList) {
        this.ifFeatureList = ifFeatureList;
    }
}
