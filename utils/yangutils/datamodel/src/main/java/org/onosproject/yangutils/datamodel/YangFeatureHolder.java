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
 * Abstraction of feature entity. It is used to abstract the data holders of feature.
 */
public interface YangFeatureHolder {

    /**
     * Returns the list of feature from data holder like container / list.
     *
     * @return the list of feature
     */
    List<YangFeature> getFeatureList();

    /**
     * Adds feature in feature list.
     *
     * @param feature the feature to be added
     */
    void addFeatureList(YangFeature feature);

    /**
     * Sets the list of feature.
     *
     * @param listOfFeature the list of feature to set
     */
    void setListOfFeature(List<YangFeature> listOfFeature);
}
