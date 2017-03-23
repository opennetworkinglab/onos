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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.yang.model.YangModel;
import org.onosproject.yang.runtime.DefaultModelRegistrationParam;
import org.onosproject.yang.runtime.ModelRegistrationParam;
import org.onosproject.yang.runtime.YangModelRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onosproject.yang.runtime.helperutils.YangApacheUtils.getYangModel;

/**
 * Abstract base for self-registering YANG models.
 */
@Component
public abstract class AbstractYangModelRegistrator {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Class<?> loaderClass;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected YangModelRegistry modelRegistry;

    private YangModel model;
    private ModelRegistrationParam registrationParam;

    /**
     * Creates a model registrator primed with the class-loader of the specified
     * class.
     *
     * @param loaderClass class whose class loader is to be used for locating schema data
     */
    protected AbstractYangModelRegistrator(Class<?> loaderClass) {
        this.loaderClass = loaderClass;
    }

    @Activate
    protected void activate() {
        model = getYangModel(loaderClass);
        registrationParam = DefaultModelRegistrationParam.builder()
                .setYangModel(model).build();
        modelRegistry.registerModel(registrationParam);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        modelRegistry.unregisterModel(registrationParam);
        log.info("Stopped");
    }
}
