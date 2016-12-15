/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service
    .rev20160317.mseaunievcservice.mefservices.uni.evc.evcperuni;

import org.onosproject.drivers.microsemi.yang.utils.CeVlanMapUtils;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.types
    .rev20160229.mseatypes.ServiceListType;

/**
 * A custom implementation of the DefaultEvcPerUnic - especially its Builder.
 *
 * This allows the EvcPerUniC to be modified after creation. These additions to the
 * builder are necessary because in the EA1000 YANG model the EvcPerUniC can
 * be added at separate stages
 */
public class CustomEvcPerUnic extends DefaultEvcPerUnic {
    public static EvcPerUnicBuilder builder(EvcPerUnic evcPerUnic) {
        return new EvcPerUnicBuilder(evcPerUnic);
    }

    public static EvcPerUnicBuilder builder() {
        return new EvcPerUnicBuilder();
    }

    public static class EvcPerUnicBuilder extends DefaultEvcPerUnic.EvcPerUnicBuilder {

        /**
         * Allow a new builder to be constructed
         */
        public EvcPerUnicBuilder() {
            valueLeafFlags.set(LeafIdentifier.CEVLANMAP.getLeafIndex());
            this.ceVlanMap = new ServiceListType("0");
            valueLeafFlags.set(LeafIdentifier.INGRESSBWPGROUPINDEX.getLeafIndex());
            this.ingressBwpGroupIndex = 0;
        }

        /**
         * Allow a builder to be constructed from an existing EvcPerUnic
         * @param evcPerUnic An existing EvcPerUnic
         */
        public EvcPerUnicBuilder(EvcPerUnic evcPerUnic) {
            this.ceVlanMap = evcPerUnic.ceVlanMap();
            this.evcPerUniServiceType = evcPerUnic.evcPerUniServiceType();
            this.ingressBwpGroupIndex = evcPerUnic.ingressBwpGroupIndex();
            this.tagManipulation = evcPerUnic.tagManipulation();
            this.flowMapping = evcPerUnic.flowMapping();
            this.yangEvcPerUnicOpType = evcPerUnic.yangEvcPerUnicOpType();
            this.yangAugmentedInfoMap = evcPerUnic.yangAugmentedInfoMap();
            this.selectLeafFlags = evcPerUnic.selectLeafFlags();
            this.valueLeafFlags = evcPerUnic.valueLeafFlags();
        }

        /**
         * Method to allow ceVlanMap to be modified.
         * @param additionalCeVlanMap An addition to the existing ceVlanMap
         * @return The updated builder
         */
        public EvcPerUnicBuilder addToCeVlanMap(ServiceListType additionalCeVlanMap) {
            String combinedCeVlanMap =
                    CeVlanMapUtils.combineVlanSets(ceVlanMap.string(), additionalCeVlanMap.string());
            //If it contains 0 we should remove it
            ceVlanMap = new ServiceListType(
                    CeVlanMapUtils.removeZeroIfPossible(combinedCeVlanMap));

            return this;
        }
    }
}
