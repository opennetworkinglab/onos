/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.openflow.controller.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.stream.Collectors;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableSet;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.net.DeviceId;
import org.onosproject.openflow.controller.OpenFlowClassifier;
import org.onosproject.openflow.controller.OpenFlowClassifierConfig;
import org.onosproject.openflow.controller.OpenFlowEvent;
import org.onosproject.openflow.controller.OpenFlowListener;
import org.onosproject.openflow.controller.OpenFlowService;
import org.slf4j.Logger;

import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigEvent;
import static org.onosproject.net.config.basics.SubjectFactories.DEVICE_SUBJECT_FACTORY;

/**
 * Manages the inventory of OpenFlow Classifiers in the system.
 */
@Component(immediate = true, service = OpenFlowService.class)
public class OpenFlowClassifierManager extends ListenerRegistry<OpenFlowEvent, OpenFlowListener>
    implements OpenFlowService {

    private Logger log = getLogger(getClass());

    private final InternalConfigListener listener = new InternalConfigListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry cfgRegistry;

    private static final String FEATURE_NAME = "classifiers";
    private final Set<ConfigFactory<?, ?>> factories = ImmutableSet.of(
            new ConfigFactory<DeviceId, OpenFlowClassifierConfig>(DEVICE_SUBJECT_FACTORY,
                    OpenFlowClassifierConfig.class, FEATURE_NAME,
                    true) {
                @Override
                public OpenFlowClassifierConfig createConfig() {
                    return new OpenFlowClassifierConfig();
                }
            }
    );

    private final Map<DeviceId, Set<OpenFlowClassifier>> classifiersMap = Maps.newConcurrentMap();

    @Activate
    private void activate() {
        factories.forEach(cfgRegistry::registerConfigFactory);
        cfgService.addListener(listener);

        for (DeviceId subject : cfgService.getSubjects(DeviceId.class, OpenFlowClassifierConfig.class)) {
            OpenFlowClassifierConfig config = cfgService.getConfig(subject, OpenFlowClassifierConfig.class);

            if (config != null) {
                updateClassifiers(config);
            }
        }

        log.info("Started Openflow Manager");
    }

    @Deactivate
    private void deactivate() {
        cfgService.removeListener(listener);

        factories.forEach(cfgRegistry::unregisterConfigFactory);
        log.info("Stopped Openflow manager");

    }

    @Override
    public void add(OpenFlowClassifier classifier) {
        checkNotNull(classifier, "Classifier cannot be null");

        OpenFlowClassifierConfig config =
                cfgService.addConfig(classifier.deviceId(), OpenFlowClassifierConfig.class);
        config.addClassifier(classifier);
        cfgService.applyConfig(classifier.deviceId(), OpenFlowClassifierConfig.class, config.node());
    }

    @Override
    public void remove(OpenFlowClassifier classifier) {
        checkNotNull(classifier, "Classifier cannot be null");

        OpenFlowClassifierConfig config = cfgService.getConfig(classifier.deviceId(), OpenFlowClassifierConfig.class);

        if (config == null) {
            return;
        }

        config.removeClassifier(classifier);
        cfgService.applyConfig(classifier.deviceId(), OpenFlowClassifierConfig.class, config.node());
    }

    @Override
    public Set<OpenFlowClassifier> getClassifiers() {
        Set<OpenFlowClassifier> classifiers = Sets.newHashSet();

        classifiersMap.values().forEach(c -> classifiers.addAll(c));

        return classifiers;
    }

    @Override
    public Set<OpenFlowClassifier> getClassifiersByDeviceId(DeviceId deviceId) {
        return classifiersMap.get(deviceId);
    }

    @Override
    public Set<OpenFlowClassifier> getClassifiersByDeviceIdAndQueue(DeviceId deviceId, int idQueue) {
        Set<OpenFlowClassifier> classifiers = classifiersMap.get(deviceId);
        if (classifiers == null) {
            return null;
        } else {
            return classifiers.stream()
                   .filter(p -> p.idQueue() == idQueue)
                   .collect(Collectors.toSet());
        }
    }

    private void updateClassifiers(OpenFlowClassifierConfig classfConfig) {
        Set<OpenFlowClassifier> old = classifiersMap.put(classfConfig.subject(),
                                                         Sets.newHashSet(classfConfig.getClassifiers()));

        if (old == null) {
            old = Collections.emptySet();
        }

        for (OpenFlowClassifier classf : classfConfig.getClassifiers()) {
            if (old.contains(classf)) {
                old.remove(classf);
            } else {
                process(new OpenFlowEvent(OpenFlowEvent.Type.INSERT, classf));
            }
        }

        for (OpenFlowClassifier classf : old) {
            process(new OpenFlowEvent(OpenFlowEvent.Type.REMOVE, classf));
        }
    }

    private void removeClassifiers(DeviceId deviceId) {
        Set<OpenFlowClassifier> old = classifiersMap.remove(deviceId);

        for (OpenFlowClassifier classf : old) {
            process(new OpenFlowEvent(OpenFlowEvent.Type.REMOVE, classf));
        }
    }

    /**
     * Listener for network config events.
     */
    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            if (event.configClass() == OpenFlowClassifierConfig.class) {
                switch (event.type()) {
                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                    event.config().ifPresent(config -> updateClassifiers((OpenFlowClassifierConfig) config));
                    break;
                case CONFIG_REMOVED:
                    removeClassifiers((DeviceId) event.subject());
                    break;
                case CONFIG_REGISTERED:
                case CONFIG_UNREGISTERED:
                default:
                    break;
                }
            }
        }
    }
}
