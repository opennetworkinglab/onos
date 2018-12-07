/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.odtn.utils.openconfig;

import org.onosproject.yang.gen.v1.openconfigplatform.rev20180603.openconfigplatform.platformcomponenttop.DefaultComponents;
import org.onosproject.yang.model.ModelObject;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.runtime.AnnotatedNodeInfo;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import com.google.common.collect.ImmutableList;

import static org.onosproject.odtn.utils.YangToolUtil.toCharSequence;
import static org.onosproject.odtn.utils.YangToolUtil.toCompositeData;
import static org.onosproject.odtn.utils.YangToolUtil.toDataNode;
import static org.onosproject.odtn.utils.YangToolUtil.toResourceData;
import static org.onosproject.odtn.utils.YangToolUtil.toXmlCompositeStream;

/**
 * Utility class to deal with OPENCONFIG Componets ModelObject & Annotation.
 */
public final class OpenConfigComponentsHandler
        extends OpenConfigObjectHandler<DefaultComponents> {

    private static final String OPENCONFIG_NAME = "components";
    private static final String NAME_SPACE = "http://openconfig.net/yang/platform";

    /**
     * OpenConfigComponentsHandler Constructor.
     */
    public OpenConfigComponentsHandler() {
        modelObject = new DefaultComponents();
        setResourceId(OPENCONFIG_NAME, NAME_SPACE, null, null);
        annotatedNodeInfos = new ArrayList<AnnotatedNodeInfo>();
    }

    /**
     * Add Component to modelObject of target OPENCONFIG.
     *
     * @param component OpenConfigComponentHandler having Component to be set for modelObject
     * @return OpenConfigComponentsHandler of target OPENCONFIG
     */
    public OpenConfigComponentsHandler addComponent(OpenConfigComponentHandler component) {
        modelObject.addToComponent(component.getModelObject());
        return this;
    }

    /**
     * Get List<CharSequence> of target OPENCONFIG.
     *
     * @return List<CharSequence> of target OPENCONFIG
     */
    public List<CharSequence> getListCharSequence() {
        return ImmutableList.of(toDataNode((ModelObject) getModelObject())).stream()
                            .map(node -> toCharSequence(toXmlCompositeStream(
                                         toCompositeData(toResourceData(
                                                         ResourceId.builder().build(), node),
                                         annotatedNodeInfos))))
                            .collect(Collectors.toList());
    }
}
