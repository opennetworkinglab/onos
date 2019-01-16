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
import java.util.UUID;
import org.apache.commons.lang.NotImplementedException;
import org.onlab.util.XmlString;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.config.FailedException;
import org.onosproject.config.Filter;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.Uuid;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.DefaultModelObjectData;
import org.onosproject.yang.model.DefaultResourceData;
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
 * Utility abstract class to deal with TAPI ModelObject with DCS.
 *
 * @param <T> modelObject to be dealt with
 */
public abstract class TapiObjectHandler<T extends ModelObject> {

    public static final String ONOS_CP = "onos-cp";
    public static final String DEVICE_ID = "device-id";
    public static final String ODTN_PORT_TYPE = "odtn-port-type";

    protected final Logger log = getLogger(getClass());

    protected ModelConverter modelConverter;
    protected DynamicConfigService dcs;

    protected T obj;

    /**
     * Get modelObject uuid.
     *
     * @return Uuid
     */
    public Uuid getId() {
        return getIdDetail();
    }

    /**
     * Generate and set modelObject uuid.
     */
    public void setId() {
        Uuid uuid = Uuid.of(UUID.randomUUID().toString());
        setIdDetail(uuid);
    }

    /**
     * Set modelObject uuid.
     *
     * @param uuid Uuid
     */
    public void setId(Uuid uuid) {
        setIdDetail(uuid);
    }

    /**
     * Get modelObject uuid, to be implemented in sub classes.
     *
     * @return Uuid
     */
    protected abstract Uuid getIdDetail();

    /**
     * Set modelObject uuid, to be implemented in sub classes.
     *
     * @param uuid Uuid
     */
    protected abstract void setIdDetail(Uuid uuid);

    /**
     * Generate DCS modelObjectId for parent node.
     *
     * @return ModelObjectId of parent node
     */
    public abstract ModelObjectId getParentModelObjectId();

    /**
     * Get modelObject instance.
     *
     * @return ModelObject of target node
     */
    public T getModelObject() {
        return obj;
    }

    /**
     * Set modelObject instance.
     *
     * @param newObj ModelObject to be set
     */
    public void setModelObject(T newObj) {
        obj = newObj;
    }

    /**
     * Get modelObjectData instance.
     *
     * @return ModelObjectData of target node
     */
    public ModelObjectData getModelObjectData() {
        ModelObject obj = getModelObject();
        ModelObjectId objId = getParentModelObjectId();

        return DefaultModelObjectData.builder()
                .addModelObject(obj)
                .identifier(objId)
                .build();
    }

    /**
     * Get modelObjectData instance for child node.
     * <p>
     * This modelObjectData is needed for read / update / delete operation
     * to extract ResourceId of this modelObject itself.
     * It's just workaround, fix in DCS needed.
     *
     * @return ModelObjectData of build target
     */
    public ModelObjectData getChildModelObjectData() {
        throw new NotImplementedException();
    }

    /**
     * Get DataNode instance.
     *
     * @return DataNode of target node
     */
    public DataNode getDataNode() {
        ResourceData rData = toResourceData(getModelObjectData());
        if (rData.dataNodes().size() > 1) {
            throw new IllegalStateException("Multiple dataNode found.");
        }
        return rData.dataNodes().get(0);
    }

    /**
     * Read modelObject from Dcs store.
     *
     * @return ModelObject
     */
    public T read() {
        return readOnDcs();
    }

    /**
     * Add modelObject to Dcs store.
     */
    public void add() {
        createOnDcs();
    }

    /**
     * Delete modelObject from Dcs store.
     */
    public void remove() {
        deleteOnDcs();
    }

    private void dcsSetup() {
        dcs = getService(DynamicConfigService.class);
        modelConverter = getService(ModelConverter.class);
    }

    @SuppressWarnings("unchecked")
    private T readOnDcs() {
        dcsSetup();
        ResourceData rData1 = toResourceData(getChildModelObjectData());
        ResourceData rData2 = toResourceData(getModelObjectData());
        DataNode rNode = dcs.readNode(rData1.resourceId(), Filter.builder().build());
        obj = toModelObject(rNode, rData2.resourceId());
        return obj;
    }

    private void createOnDcs() {
        dcsSetup();
        ResourceData rData = toResourceData(getModelObjectData());
        addResourceDataToDcs(rData, rData.resourceId());
    }

    private void deleteOnDcs() {
        dcsSetup();
        ResourceData rData = toResourceData(getChildModelObjectData());
        dcs.deleteNode(rData.resourceId());
    }

    private void addResourceDataToDcs(ResourceData input, ResourceId rid) {
        if (input == null || input.dataNodes() == null) {
            return;
        }
        List<DataNode> dataNodes = input.dataNodes();
        for (DataNode node : dataNodes) {
            try {
                dcs.createNode(rid, node);
            } catch (FailedException e) {
                    log.warn("Failed to add resource {}", rid);
                    log.debug("Exception", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected T toModelObject(DataNode rNode, ResourceId rId) {
        dcsSetup();
        ResourceData rData = toResourceData(rNode, rId);
        ModelObjectData modelObjectData = modelConverter.createModel(rData);
        if (modelObjectData.modelObjects().size() > 1) {
            throw new IllegalStateException("Multiple modelObject found.");
        }
        if (modelObjectData.modelObjects().isEmpty()) {
            throw new IllegalStateException("ModelObject must not be empty.");
        }
        return (T) modelObjectData.modelObjects().get(0);
    }

    private ResourceData toResourceData(DataNode rNode, ResourceId rId) {
        return DefaultResourceData.builder()
                .addDataNode(rNode)
                .resourceId(rId)
                .build();
    }

    private ResourceData toResourceData(ModelObjectData data) {
        dcsSetup();
        ResourceData rData = modelConverter.createDataNode(data);

        // for debug
        CharSequence strNode = toCharSequence(toXmlCompositeStream(toCompositeData(rData)));
        log.debug("XML:\n{}", XmlString.prettifyXml(strNode));

        return rData;
    }

}
