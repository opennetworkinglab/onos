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

import org.onosproject.yang.model.KeyLeaf;
import org.onosproject.yang.model.ModelObject;
import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.runtime.AnnotatedNodeInfo;
import org.onosproject.yang.runtime.Annotation;
import org.onosproject.yang.runtime.DefaultAnnotatedNodeInfo;
import org.onosproject.yang.runtime.DefaultAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/**
 * Utility abstract class to deal with OPENCONFIG ModelObject & Annotation.
 *
 * @param <O> modelOject to be dealt with
 */
public abstract class OpenConfigObjectHandler<O extends ModelObject> {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected O modelObject;
    protected ResourceId resourceId;
    protected List<AnnotatedNodeInfo> annotatedNodeInfos;

    /**
     * Get modelObject instance.
     *
     * @return ModelObject of target OPENCONFIG
     */
    public O getModelObject() {
        return modelObject;
    }

    /**
     * Get resourceId instance.
     *
     * @return ResourceId of target OPENCONFIG
     */
    public ResourceId getResourceId() {
        return resourceId;
    }

    /**
     * Set resourceId instance.
     *
     * @param openconfigName String of target OPENCONFIG name
     * @param openconfigNameSpace String of target OPENCONFIG namespace
     * @param keyLeaf KeyLeaf of target OPENCONFIG
     * @param pBuilder ResourceId.Builder of parent OPENCONFIG
     */
    protected void setResourceId(String openconfigName, String openconfigNameSpace,
                                        KeyLeaf keyLeaf, ResourceId.Builder pBuilder) {
        ResourceId.Builder ridBuilder = ResourceId.builder();
        if (pBuilder != null) {
            ridBuilder = pBuilder.addBranchPointSchema(openconfigName, openconfigNameSpace);
        } else {
            ridBuilder = ridBuilder.addBranchPointSchema(openconfigName,
                                                         openconfigNameSpace);
        }

        if (keyLeaf != null) {
            ridBuilder = ridBuilder.addKeyLeaf(
                             keyLeaf.leafSchema().name(),
                             keyLeaf.leafSchema().namespace(),
                             keyLeaf.leafValue());
        }

        resourceId = ridBuilder.build();
    }

    /**
     * Get ridBuilder instance.
     *
     * @return ResourceId.Builder of target OPENCONFIG
     */
    public ResourceId.Builder getResourceIdBuilder() {
        try {
            return resourceId.copyBuilder();
        } catch (CloneNotSupportedException e) {
            log.error("Exception thrown", e);
            return null;
        }
    }

    /**
     * Add Annotation for annotaedNodeinfos.
     *
     * @param key String of Annotation's key
     * @param value String of Annotation's value
     */
    public void addAnnotation(String key, String value) {
        AnnotatedNodeInfo.Builder builder = DefaultAnnotatedNodeInfo.builder();

        AnnotatedNodeInfo preAnnotate = annotatedNodeInfos.stream()
                                        .filter(annotatedNodeInfo -> annotatedNodeInfo.resourceId()
                                                                     .equals(resourceId))
                                        .findFirst()
                                        .orElse(null);

        if (preAnnotate != null) {
             annotatedNodeInfos.remove(preAnnotate);
             for (Annotation annotation : preAnnotate.annotations()) {
                  builder.addAnnotation(annotation);
             }
        }

        annotatedNodeInfos.add(builder.addAnnotation(new DefaultAnnotation(key, value))
                                      .resourceId(resourceId)
                                      .build());
    }

    /**
     * Get annotatedNodeInfos instance.
     *
     * @return List<AnnotatedNodeInfo> of all OPENCONFIG
     */
    public List<AnnotatedNodeInfo> getAnnotatedNodeInfoList() {
        return annotatedNodeInfos;
    }
}
