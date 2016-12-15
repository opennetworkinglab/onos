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
    .rev20160317.mseaunievcservice.mefservices.uni.evc;

import org.onosproject.drivers.microsemi.yang.UniSide;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.types
    .rev20160229.mseatypes.ServiceListType;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.evcperuniextensionattributes.FlowMapping;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service
    .rev20160317.mseaunievcservice.mefservices.uni.evc.evcperuni.CustomEvcPerUnic;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service
    .rev20160317.mseaunievcservice.mefservices.uni.evc.evcperuni.CustomEvcPerUnin;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.uni.evc.evcperuni.EvcPerUnic;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.uni.evc.evcperuni.EvcPerUnin;

/**
 * A custom implementation of the DefaultEvcPerUni - especially its Builder.
 *
 * This allows the EvcPerUni to be modified after creation. These additions to the
 * builder are necessary because in the EA1000 YANG model the EvcPerUni side can
 * be added at separate stages
 */
public class CustomEvcPerUni extends DefaultEvcPerUni {

    public static EvcPerUniBuilder builder(EvcPerUni evcPerUni) {
        return new EvcPerUniBuilder(evcPerUni);
    }

    public static class EvcPerUniBuilder extends DefaultEvcPerUni.EvcPerUniBuilder {

        /**
         * Allow a builder to be constructed from an existing EvcPerUni
         * @param evcPerUni An existing EvcPerUni
         */
        public EvcPerUniBuilder(EvcPerUni evcPerUni) {
            this.evcPerUnic = evcPerUni.evcPerUnic();
            this.evcPerUnin = evcPerUni.evcPerUnin();
            this.evcUniType = evcPerUni.evcUniType();
            this.yangEvcPerUniOpType = evcPerUni.yangEvcPerUniOpType();
            this.valueLeafFlags = evcPerUni.valueLeafFlags();
            this.selectLeafFlags = evcPerUni.selectLeafFlags();
            this.yangAugmentedInfoMap = evcPerUni.yangAugmentedInfoMap();
        }

        /**
         * Method to allow ceVlanMap to be modified.
         * @param additionalCeVlanMap An addition to the existing ceVlanMap
         * @param side The Uni Side - Customer or Network
         * @return The updated builder
         */
        public EvcPerUniBuilder addToCeVlanMap(ServiceListType additionalCeVlanMap, UniSide side) {
            if (side == UniSide.NETWORK) {
                evcPerUnin = CustomEvcPerUnin.builder(evcPerUnin).addToCeVlanMap(additionalCeVlanMap).build();
            } else {
                evcPerUnic = CustomEvcPerUnic.builder(evcPerUnic).addToCeVlanMap(additionalCeVlanMap).build();
            }

            return this;
        }

        /**
         * Method to allow the Flow Mapping list to be modified.
         * @param fm the flow mapping
         * @param side The Uni Side - Customer or Network
         * @return The updated builder
         */
        public EvcPerUniBuilder addToFlowMapping(FlowMapping fm, UniSide side) {
            if (side == UniSide.NETWORK) {
                evcPerUnin = CustomEvcPerUnin.builder(evcPerUnin).addToFlowMapping(fm).build();
            } else {
                evcPerUnic = CustomEvcPerUnic.builder(evcPerUnic).addToFlowMapping(fm).build();
            }
            return this;
        }

        /**
         * Method to allow an EVC side to be added.
         * @param evcUniN An EVCPerUni object
         * @return The updated builder
         */
        public EvcPerUniBuilder addUniN(EvcPerUnin evcUniN) {
            this.evcPerUnin = evcUniN;
            return this;
        }

        /**
         * Method to allow an EVC side to be added.
         * @param evcUniC An EVCPerUni object
         * @return The updated builder
         */
        public EvcPerUniBuilder addUniC(EvcPerUnic evcUniC) {
            this.evcPerUnic = evcUniC;
            return this;
        }
    }
}
