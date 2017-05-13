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

import com.google.common.annotations.Beta;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.CoreService;
import org.onosproject.yang.compiler.api.YangCompilationParam;
import org.onosproject.yang.compiler.api.YangCompiledOutput;
import org.onosproject.yang.compiler.api.YangCompilerException;
import org.onosproject.yang.compiler.api.YangCompilerService;
import org.onosproject.yang.compiler.tool.YangCompilerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Represents implementation of YANG live compiler manager.
 */
@Beta
@Service
@Component(immediate = true)
public class YangLiveCompilerManager implements YangCompilerService {

    private static final String APP_ID = "org.onosproject.yang";
    private final Logger log = LoggerFactory.getLogger(getClass());
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Activate
    public void activate() {
        coreService.registerApplication(APP_ID);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public YangCompiledOutput compileYangFiles(YangCompilationParam param)
            throws IOException, YangCompilerException {
        return new YangCompilerManager().compileYangFiles(param);
    }
}
