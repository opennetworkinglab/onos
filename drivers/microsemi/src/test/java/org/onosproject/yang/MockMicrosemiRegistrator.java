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
package org.onosproject.yang;

import org.onosproject.drivers.microsemi.yang.MicrosemiModelRegistrator;
import org.onosproject.yang.compiler.datamodel.YangNode;
import org.onosproject.yang.compiler.datamodel.utils.DataModelUtils;
import org.onosproject.yang.runtime.DefaultModelRegistrationParam;
import org.onosproject.yang.runtime.ModelRegistrationParam;
import org.onosproject.yang.runtime.YangModelRegistry;
import org.onosproject.yang.runtime.helperutils.YangApacheUtils;
import org.onosproject.yang.runtime.impl.DefaultYangModelRegistry;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MockMicrosemiRegistrator extends MicrosemiModelRegistrator {
    private static final String FS = File.separator;
    private static final String PATH = System.getProperty("user.dir") +
            FS + "buck-out" + FS + "gen" +
            FS + "models" + FS + "microsemi" + FS + "onos-models-microsemi-schema" + FS;
    private static final String SER_FILE_PATH = "yang" + FS + "resources" +
            FS + "YangMetaData.ser";
    private static final String META_PATH =
            PATH.replace("drivers/microsemi", "")
                    + SER_FILE_PATH;

    @Override
    public void activate() {
        modelRegistry = new DefaultYangModelRegistry();
        List<YangNode> nodes = new ArrayList<>();
        try {
            nodes.addAll(DataModelUtils.deSerializeDataModel(META_PATH));

            model = YangApacheUtils.processYangModel(META_PATH, nodes);
            ModelRegistrationParam.Builder b =
                    DefaultModelRegistrationParam.builder().setYangModel(model);
            b.setYangModel(model);

            ModelRegistrationParam registrationParam = getAppInfo(b).setYangModel(model).build();
            modelRegistry.registerModel(registrationParam);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public YangModelRegistry registry() {
        return modelRegistry;
    }
}
