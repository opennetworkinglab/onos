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

package org.onosproject.odtn.utils.tapi;

import java.util.List;
import org.onlab.util.XmlString;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.Uuid;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.DefaultModelObjectData;
import org.onosproject.yang.model.ModelConverter;
import org.onosproject.yang.model.ModelObject;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ModelObjectId;
import org.onosproject.yang.model.ResourceData;

import static org.onlab.osgi.DefaultServiceDirectory.getService;
import static org.onosproject.odtn.utils.YangToolUtil.toCharSequence;
import static org.onosproject.odtn.utils.YangToolUtil.toCompositeData;
import static org.onosproject.odtn.utils.YangToolUtil.toXmlCompositeStream;

import org.onosproject.yang.model.ResourceId;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Utility builder class for TAPI modelobject creation with DCS.
 */
public abstract class TapiInstanceBuilder {

    public static final String ONOS_CP = "onos-cp";

    public static final String DEVICE_ID = "device-id";

    public static final String ODTN_PORT_TYPE = "odtn-port-type";

    private final Logger log = getLogger(getClass());

    private ModelConverter modelConverter;
    private DynamicConfigService dcs;

    /**
     * Generate DCS modelObjectData.
     *
     * @return ModelObjectId of build target
     */
    public abstract ModelObjectId getModelObjectId();

    /**
     * Get modelObject instance.
     *
     * @param <T> build target class
     * @return ModelObject of build target
     */
    public abstract <T extends ModelObject> T getModelObject();

    /**
     * Get modelObject uuid.
     *
     * @return Uuid of build target
     */
    public abstract Uuid getUuid();

    /**
     * Get modelObjectData instance.
     *
     * @return ModelObjectData of build target
     */
    public ModelObjectData getModelObjectData() {
        ModelObject obj = getModelObject();
        ModelObjectId objId = getModelObjectId();

        return DefaultModelObjectData.builder()
                .addModelObject(obj)
                .identifier(objId)
                .build();
    }

    /**
     * Add built modelObject to Dcs store.
     */
    public void build() {
        dcs = getService(DynamicConfigService.class);
        modelConverter = getService(ModelConverter.class);
        addModelObjectDataToDcs(getModelObjectData());
    }

    private void addModelObjectDataToDcs(ModelObjectData input) {

        ResourceData rnode = modelConverter.createDataNode(input);

        // for debug
        CharSequence strNode = toCharSequence(toXmlCompositeStream(toCompositeData(rnode)));
        log.info("XML:\n{}", XmlString.prettifyXml(strNode));

        addResourceDataToDcs(rnode);
    }

    private void addResourceDataToDcs(ResourceData input) {
        addResourceDataToDcs(input, input.resourceId());
    }

    private void addResourceDataToDcs(ResourceData input, ResourceId rid) {
        if (input == null || input.dataNodes() == null) {
            return;
        }
        List<DataNode> dataNodes = input.dataNodes();
        for (DataNode node : dataNodes) {
            dcs.createNode(rid, node);
        }
    }

}
