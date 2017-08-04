/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Test adapter for network configuration service.
 */
public class NetworkConfigServiceAdapter implements NetworkConfigService {
    @Override
    public Set<Class> getSubjectClasses() {
        return ImmutableSet.of();
    }

    @Override
    public SubjectFactory getSubjectFactory(String subjectClassKey) {
        return null;
    }

    @Override
    public SubjectFactory getSubjectFactory(Class subjectClass) {
        return null;
    }

    @Override
    public Class<? extends Config> getConfigClass(String subjectClassKey, String configKey) {
        return null;
    }

    @Override
    public <S> Set<S> getSubjects(Class<S> subjectClass) {
        return ImmutableSet.of();
    }

    @Override
    public <S, C extends Config<S>> Set<S> getSubjects(Class<S> subjectClass, Class<C> configClass) {
        return ImmutableSet.of();
    }

    @Override
    public <S> Set<? extends Config<S>> getConfigs(S subject) {
        return ImmutableSet.of();
    }

    @Override
    public <S, C extends Config<S>> C getConfig(S subject, Class<C> configClass) {
        return null;
    }

    @Override
    public <S, C extends Config<S>> C addConfig(S subject, Class<C> configClass) {
        return null;
    }

    @Override
    public <S, C extends Config<S>> C applyConfig(S subject, Class<C> configClass, JsonNode json) {
        return null;
    }

    @Override
    public <S, C extends Config<S>> C applyConfig(String subjectClassKey, S subject, String configKey, JsonNode json) {
        return null;
    }

    @Override
    public <S, C extends Config<S>> void removeConfig(S subject, Class<C> configClass) {

    }

    @Override
    public <S> void removeConfig(String subjectClassKey, S subject, String configKey) {
    }

    @Override
    public void addListener(NetworkConfigListener listener) {
    }

    @Override
    public void removeListener(NetworkConfigListener listener) {
    }

    @Override
    public <S> void removeConfig(S subject) {
    }

    @Override
    public <S> void removeConfig() {
    }
}
