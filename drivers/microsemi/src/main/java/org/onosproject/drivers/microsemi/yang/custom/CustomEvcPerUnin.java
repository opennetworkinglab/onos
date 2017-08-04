/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.drivers.microsemi.yang.custom;

import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.ServiceListType;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.evcperuniextensionattributes.EvcPerUniServiceTypeEnum;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.evcperuniextensionattributes.FlowMapping;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.evcperuniextensionattributes.TagManipulation;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.uni.evc.evcperuni.DefaultEvcPerUnin;

import java.util.List;

/**
 * Extend the DefaultEvcPerUnin so that the ceVlanMap can always be initialized at 0.
 */
public class CustomEvcPerUnin extends DefaultEvcPerUnin {
    @Override
    public ServiceListType ceVlanMap() {
        if (ceVlanMap == null) {
            return new ServiceListType("0");
        }
        return ceVlanMap;
    }
    @Override
    public Object ingressBwpGroupIndex() {
        return ingressBwpGroupIndex;
    }

    @Override
    public EvcPerUniServiceTypeEnum evcPerUniServiceType() {
        return evcPerUniServiceType;
    }

    @Override
    public TagManipulation tagManipulation() {
        return tagManipulation;
    }

    @Override
    public List<FlowMapping> flowMapping() {
        return flowMapping;
    }

}
