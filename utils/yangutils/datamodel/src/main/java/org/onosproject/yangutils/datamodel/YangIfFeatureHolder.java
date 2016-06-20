/*Copyright 2016.year Open Networking Laboratory

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

package org.onosproject.yangutils.datamodel;

import java.util.List;

/**
 * Abstraction of if-feature entity. It is used to abstract the data holders of if-feature.
 */
public interface YangIfFeatureHolder {

    /**
     * Returns the list of if-feature from data holder like container / list.
     *
     * @return the list of if-feature
     */
    List<YangIfFeature> getIfFeatureList();

    /**
     * Adds if-feature in if-feature list.
     *
     * @param ifFeature the if-feature to be added
     */
    void addIfFeatureList(YangIfFeature ifFeature);

    /**
     * Sets the list of if-feature.
     *
     * @param ifFeatureList the list of if-feature to set
     */
    void setIfFeatureList(List<YangIfFeature> ifFeatureList);
}
