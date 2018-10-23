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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onosproject.yang.model.YangModel;
import org.onosproject.yang.model.YangModuleId;
import org.onosproject.yang.runtime.AppModuleInfo;
import org.onosproject.yang.runtime.DefaultModelRegistrationParam;
import org.onosproject.yang.runtime.ModelRegistrationParam;
import org.onosproject.yang.runtime.ModelRegistrationParam.Builder;
import org.onosproject.yang.runtime.YangModelRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.onosproject.yang.runtime.helperutils.YangApacheUtils.getYangModel;

/**
 * Abstract base for self-registering YANG models.
 */
public abstract class AbstractYangModelRegistrator {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    private final Class<?> loaderClass;

    protected Map<YangModuleId, AppModuleInfo> appInfo;
    protected YangModel model;
    private ModelRegistrationParam registrationParam;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected YangModelRegistry modelRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected YangClassLoaderRegistry sourceResolver;

    /**
     * Binds the specified YANG model registry.
     *
     * @param registry model registry
     */
    protected void bindModelRegistry(YangModelRegistry registry) {
        this.modelRegistry = registry;
    }

    /**
     * Unbinds the specified YANG model registry.
     *
     * @param registry model registry
     */
    protected void unbindModelRegistry(YangModelRegistry registry) {
        this.modelRegistry = null;
    }

    /**
     * Binds the specified YANG source resolver registry.
     *
     * @param resolver model source resolver
     */
    protected void bindSourceResolver(YangClassLoaderRegistry resolver) {
        this.sourceResolver = resolver;
    }

    /**
     * Unbinds the specified YANG source resolver registry.
     *
     * @param resolver model source resolver
     */
    protected void unbindSourceResolver(YangClassLoaderRegistry resolver) {
        this.sourceResolver = null;
    }

    /**
     * Creates a model registrator primed with the class-loader of the specified
     * class.
     *
     * @param loaderClass class whose class loader is to be used for locating
     *                    schema data
     */
    protected AbstractYangModelRegistrator(Class<?> loaderClass) {
        this.loaderClass = loaderClass;
    }

    /**
     * Creates a model registrator primed with the class-loader of the specified
     * class and application info.
     *
     * @param loaderClass class whose class loader is to be used for locating
     *                    schema data
     * @param appInfo     application information
     */
    protected AbstractYangModelRegistrator(Class<?> loaderClass,
                                           Map<YangModuleId, AppModuleInfo> appInfo) {
        this.loaderClass = loaderClass;
        this.appInfo = appInfo;
    }

    @Activate
    protected void activate() {
        log.info("Starting...");
        model = getYangModel(loaderClass);
        log.info("ModelId: {}", model.getYangModelId());
        ModelRegistrationParam.Builder b =
                DefaultModelRegistrationParam.builder().setYangModel(model);
        registrationParam = getAppInfo(b).setYangModel(model).build();
        sourceResolver.registerClassLoader(model.getYangModelId(), loaderClass.getClassLoader());
        modelRegistry.registerModel(registrationParam);
        log.info("Started");
    }

    protected ModelRegistrationParam.Builder getAppInfo(Builder b) {
        if (appInfo != null) {
            appInfo.entrySet().stream().filter(
                    entry -> model.getYangModule(entry.getKey()) != null).forEach(
                    entry -> b.addAppModuleInfo(entry.getKey(), entry.getValue()));
        }
        return b;
    }

    @Deactivate
    protected void deactivate() {
        modelRegistry.unregisterModel(registrationParam);
        sourceResolver.unregisterClassLoader(model.getYangModelId());
        log.info("Stopped");
    }
}
