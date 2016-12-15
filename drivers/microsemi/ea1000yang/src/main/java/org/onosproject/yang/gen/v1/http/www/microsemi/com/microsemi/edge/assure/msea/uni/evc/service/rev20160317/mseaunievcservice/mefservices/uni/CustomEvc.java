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
    .rev20160317.mseaunievcservice.mefservices.uni;

import org.onosproject.drivers.microsemi.yang.UniSide;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.types
    .rev20160229.mseatypes.ServiceListType;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.evcperuniextensionattributes.FlowMapping;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service
    .rev20160317.mseaunievcservice.mefservices.uni.evc.CustomEvcPerUni;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.uni.evc.evcperuni.EvcPerUnic;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.uni.evc.evcperuni.EvcPerUnin;

/**
 * A custom implementation of the DefaultEvc - especially its Builder.
 *
 * This allows the Evc to be modified after creation. These additions to the
 * builder are necessary because in the EA1000 YANG model many different Open Flow
 * flows can be associated with one EVC - each one has its own Ce-Vlan-Id and
 * Flow Reference
 */
public class CustomEvc extends DefaultEvc {

    public static EvcBuilder builder(Evc evc) {
        return new EvcBuilder(evc);
    }

    public static class EvcBuilder extends DefaultEvc.EvcBuilder {

        /**
         * Allow a builder to be constructed from an existing EVC
         * @param evc An existing EVC
         */
        public EvcBuilder(Evc evc) {
            this.evcPerUni = evc.evcPerUni();
            this.evcStatus = evc.evcStatus();
            this.evcIndex = evc.evcIndex();
            this.mtuSize = evc.mtuSize();
            this.cevlanCosPreservation = evc.cevlanCosPreservation();
            this.cevlanIdPreservation = evc.cevlanIdPreservation();
            this.name = evc.name();
            this.yangEvcOpType = evc.yangEvcOpType();
            this.yangAugmentedInfoMap = evc.yangAugmentedInfoMap();
            this.name = evc.name();
            this.serviceType = evc.serviceType();
            this.selectLeafFlags = evc.selectLeafFlags();
            this.uniEvcId = evc.uniEvcId();
            this.valueLeafFlags = evc.valueLeafFlags();
        }

        /**
         * Method to allow ceVlanMap to be modified.
         * @param additionalCeVlanMap An addition to the existing ceVlanMap
         * @param side The Uni Side - Customer or Network
         * @return The updated builder
         */
        public EvcBuilder addToCeVlanMap(ServiceListType additionalCeVlanMap, UniSide side) {
            evcPerUni = CustomEvcPerUni.builder(evcPerUni).addToCeVlanMap(additionalCeVlanMap, side).build();
            return this;
        }

        /**
         * Method to allow the Flow Mapping list to be modified.
         * @param fm the flow mapping
         * @param side The Uni Side - Customer or Network
         * @return The updated builder
         */
        public EvcBuilder addToFlowMapping(FlowMapping fm, UniSide side) {
            evcPerUni = CustomEvcPerUni.builder(evcPerUni).addToFlowMapping(fm, side).build();
            return this;
        }

        /**
         * Method to allow an EVC side to be added.
         * @param evcUniN An EVCPerUni object
         * @return The updated builder
         */
        public EvcBuilder addUniN(EvcPerUnin evcUniN) {
            evcPerUni = CustomEvcPerUni.builder(evcPerUni).addUniN(evcUniN).build();
            return this;
        }

        /**
         * Method to allow an EVC side to be added.
         * @param evcUniC An EVCPerUni object
         * @return The updated builder
         */
        public EvcBuilder addUniC(EvcPerUnic evcUniC) {
            evcPerUni = CustomEvcPerUni.builder(evcPerUni).addUniC(evcUniC).build();
            return this;
        }
    }
}
