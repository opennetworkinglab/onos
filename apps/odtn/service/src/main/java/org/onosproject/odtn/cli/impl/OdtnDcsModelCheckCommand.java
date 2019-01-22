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

package org.onosproject.odtn.cli.impl;

import java.util.regex.Pattern;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.util.XmlString;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.config.Filter;
import static org.onosproject.odtn.utils.YangToolUtil.toCharSequence;
import static org.onosproject.odtn.utils.YangToolUtil.toCompositeData;
import static org.onosproject.odtn.utils.YangToolUtil.toResourceData;
import static org.onosproject.odtn.utils.YangToolUtil.toXmlCompositeStream;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.DefaultContext;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.DefaultModelObjectData;
import org.onosproject.yang.model.InnerModelObject;
import org.onosproject.yang.model.ModelConverter;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ModelObjectId;
import org.onosproject.yang.model.ResourceData;
import org.onosproject.yang.model.ResourceId;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

@Service
@Command(scope = "onos", name = "odtn-show-tapi-context",
         description = "show tapi context command")
public class OdtnDcsModelCheckCommand extends AbstractShellCommand {

    private static final Logger log = getLogger(OdtnDcsModelCheckCommand.class);
    private DynamicConfigService dcs;
    private ModelConverter modelConverter;

    private void printlog(String format, Object... objs) {
        print(format.replaceAll(Pattern.quote("{}"), "%s"), objs);
        log.debug(format, objs);
    }

    @Override
    protected void doExecute() {
        dcs = get(DynamicConfigService.class);
        modelConverter = get(ModelConverter.class);
        dumpDcsStore(DefaultContext.class);
    }

    private ResourceId getResourceId(ModelObjectId modelId) {
        ModelObjectData data = DefaultModelObjectData.builder()
                .identifier(modelId)
                .build();
        ResourceData rnode = modelConverter.createDataNode(data);
        return rnode.resourceId();
    }

    private <T extends InnerModelObject> void dumpDcsStore(Class<T> cls) {

        ModelObjectId mid = ModelObjectId.builder().addChild(cls).build();
        DataNode all = dcs.readNode(getResourceId(mid), Filter.builder().build());

        ResourceId empty = ResourceId.builder().build();
        CharSequence strNode = toCharSequence(toXmlCompositeStream(toCompositeData(toResourceData(empty, all))));
        printlog("XML:\n{}", XmlString.prettifyXml(strNode));
    }
}
