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
package org.onosproject.yang;

import com.google.common.collect.ImmutableMap;
import org.onosproject.models.microsemi.MicrosemiModelRegistrator;
import org.onosproject.yang.compiler.datamodel.YangNode;
import org.onosproject.yang.compiler.tool.YangNodeInfo;
import org.onosproject.yang.model.YangModuleId;
import org.onosproject.yang.runtime.AppModuleInfo;
import org.onosproject.yang.runtime.DefaultModelRegistrationParam;
import org.onosproject.yang.runtime.ModelRegistrationParam;
import org.onosproject.yang.runtime.YangModelRegistry;
import org.onosproject.yang.runtime.impl.DefaultYangModelRegistry;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.fail;
import static org.onosproject.yang.compiler.tool.YangCompilerManager.deSerializeDataModel;
import static org.onosproject.yang.compiler.tool.YangCompilerManager.getYangNodes;
import static org.onosproject.yang.compiler.tool.YangCompilerManager.processYangModel;

public class MockMicrosemiRegistrator extends MicrosemiModelRegistrator {
    private static final String FS = File.separator;
    private static final String PATH = System.getProperty("user.dir") +
            FS + "buck-out" + FS + "gen" +
            FS + "models" + FS + "microsemi" + FS + "onos-models-microsemi-schema" + FS;
    private static final String SER_FILE_PATH = "yang" + FS + "resources" +
            FS + "YangMetaData.ser";
    private static final String META_PATH =
            PATH.replace("drivers/microsemi/ea1000", "")
                    + SER_FILE_PATH;

    @Override
    public void activate() {
        modelRegistry = new DefaultYangModelRegistry();
        List<YangNodeInfo> nodes = new ArrayList<>();
        try {
            for (YangNode node : getYangNodes(deSerializeDataModel(META_PATH))) {
                nodes.add(new YangNodeInfo(node, false));
            }

            model = processYangModel(META_PATH, nodes, "test", false);
            ModelRegistrationParam.Builder b =
                    DefaultModelRegistrationParam.builder().setYangModel(model);
            b.setYangModel(model);

            ModelRegistrationParam registrationParam = getAppInfo(b).setYangModel(model).build();
            modelRegistry.registerModel(registrationParam);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            fail();
        }
    }


    public void addAppInfo(Map<YangModuleId, AppModuleInfo> map) {
        Map<YangModuleId, AppModuleInfo> appInfoCopy = new HashMap<>();
        appInfoCopy.putAll(appInfo);
        appInfoCopy.putAll(map);
        appInfo = ImmutableMap.copyOf(appInfoCopy);
    }

    public YangModelRegistry registry() {
        return modelRegistry;
    }
}
