/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.config.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ShortNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigStore;
import org.onosproject.net.config.NetworkConfigStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.ConsistentMapException;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.onosproject.net.config.NetworkConfigEvent.Type.*;

/**
 * Implementation of a distributed network configuration store.
 */
@Component(immediate = true)
@Service
public class DistributedNetworkConfigStore
        extends AbstractStore<NetworkConfigEvent, NetworkConfigStoreDelegate>
        implements NetworkConfigStore {

    private static final int MAX_BACKOFF = 10;

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private ConsistentMap<ConfigKey, JsonNode> configs;

    private final Map<String, ConfigFactory> factoriesByConfig = Maps.newConcurrentMap();
    private final ObjectMapper mapper = new ObjectMapper();
    private final ConfigApplyDelegate applyDelegate = new InternalApplyDelegate();
    private final MapEventListener<ConfigKey, JsonNode> listener = new InternalMapListener();

    @Activate
    public void activate() {
        KryoNamespace.Builder kryoBuilder = new KryoNamespace.Builder()
                .register(KryoNamespaces.API)
                .register(ConfigKey.class, ObjectNode.class, ArrayNode.class,
                          JsonNodeFactory.class, LinkedHashMap.class,
                          TextNode.class, BooleanNode.class,
                          LongNode.class, DoubleNode.class, ShortNode.class, IntNode.class);

        configs = storageService.<ConfigKey, JsonNode>consistentMapBuilder()
                .withSerializer(Serializer.using(kryoBuilder.build()))
                .withName("onos-network-configs")
                .withRelaxedReadConsistency()
                .build();
        configs.addListener(listener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        configs.removeListener(listener);
        log.info("Stopped");
    }

    @Override
    public void addConfigFactory(ConfigFactory configFactory) {
        factoriesByConfig.put(configFactory.configClass().getName(), configFactory);
        notifyDelegate(new NetworkConfigEvent(CONFIG_REGISTERED, configFactory.configKey(),
                                              configFactory.configClass()));
    }

    @Override
    public void removeConfigFactory(ConfigFactory configFactory) {
        factoriesByConfig.remove(configFactory.configClass().getName());
        notifyDelegate(new NetworkConfigEvent(CONFIG_UNREGISTERED, configFactory.configKey(),
                                              configFactory.configClass()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S, C extends Config<S>> ConfigFactory<S, C> getConfigFactory(Class<C> configClass) {
        return (ConfigFactory<S, C>) factoriesByConfig.get(configClass.getName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S> Set<S> getSubjects(Class<S> subjectClass) {
        ImmutableSet.Builder<S> builder = ImmutableSet.builder();
        configs.keySet().forEach(k -> {
            if (subjectClass.isInstance(k.subject)) {
                builder.add((S) k.subject);
            }
        });
        return builder.build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S, C extends Config<S>> Set<S> getSubjects(Class<S> subjectClass, Class<C> configClass) {
        ImmutableSet.Builder<S> builder = ImmutableSet.builder();
        String cName = configClass.getName();
        configs.keySet().forEach(k -> {
            if (subjectClass.isInstance(k.subject) && cName.equals(k.configClass)) {
                builder.add((S) k.subject);
            }
        });
        return builder.build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S> Set<Class<? extends Config<S>>> getConfigClasses(S subject) {
        ImmutableSet.Builder<Class<? extends Config<S>>> builder = ImmutableSet.builder();
        configs.keySet().forEach(k -> {
            if (Objects.equals(subject, k.subject) && delegate != null) {
                builder.add(factoriesByConfig.get(k.configClass).configClass());
            }
        });
        return builder.build();
    }

    @Override
    public <S, T extends Config<S>> T getConfig(S subject, Class<T> configClass) {
        // TODO: need to identify and address the root cause for timeouts.
        Versioned<JsonNode> json = Tools.retryable(configs::get, ConsistentMapException.class, 1, MAX_BACKOFF)
                .apply(key(subject, configClass));
        return json != null ? createConfig(subject, configClass, json.value()) : null;
    }


    @Override
    public <S, C extends Config<S>> C createConfig(S subject, Class<C> configClass) {
        ConfigFactory<S, C> factory = getConfigFactory(configClass);
        Versioned<JsonNode> json = configs.computeIfAbsent(key(subject, configClass),
                                                             k -> factory.isList() ?
                                                                     mapper.createArrayNode() :
                                                                     mapper.createObjectNode());
        return createConfig(subject, configClass, json.value());
    }

    @Override
    public <S, C extends Config<S>> C applyConfig(S subject, Class<C> configClass, JsonNode json) {
        return createConfig(subject, configClass,
                            configs.putAndGet(key(subject, configClass), json).value());
    }

    @Override
    public <S, C extends Config<S>> void clearConfig(S subject, Class<C> configClass) {
        configs.remove(key(subject, configClass));
    }

    /**
     * Produces a config from the specified subject, config class and raw JSON.
     *
     * @param subject     config subject
     * @param configClass config class
     * @param json        raw JSON data
     * @return config object or null of no factory found or if the specified
     * JSON is null
     */
    @SuppressWarnings("unchecked")
    private <S, C extends Config<S>> C createConfig(S subject, Class<C> configClass,
                                                    JsonNode json) {
        if (json != null) {
            ConfigFactory<S, C> factory = factoriesByConfig.get(configClass.getName());
            if (factory != null) {
                C config = factory.createConfig();
                config.init(subject, factory.configKey(), json, mapper, applyDelegate);
                return config;
            }
        }
        return null;
    }


    // Auxiliary delegate to receive notifications about changes applied to
    // the network configuration - by the apps.
    private class InternalApplyDelegate implements ConfigApplyDelegate {
        @Override
        public void onApply(Config config) {
            configs.put(key(config.subject(), config.getClass()), config.node());
        }
    }

    // Produces a key for uniquely tracking a subject config.
    private static ConfigKey key(Object subject, Class<?> configClass) {
        return new ConfigKey(subject, configClass);
    }

    // Auxiliary key to track subject configurations.
    private static final class ConfigKey {
        final Object subject;
        final String configClass;

        private ConfigKey(Object subject, Class<?> configClass) {
            this.subject = subject;
            this.configClass = configClass.getName();
        }

        @Override
        public int hashCode() {
            return Objects.hash(subject, configClass);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ConfigKey) {
                final ConfigKey other = (ConfigKey) obj;
                return Objects.equals(this.subject, other.subject)
                        && Objects.equals(this.configClass, other.configClass);
            }
            return false;
        }
    }

    private class InternalMapListener implements MapEventListener<ConfigKey, JsonNode> {
        @Override
        public void event(MapEvent<ConfigKey, JsonNode> event) {
            NetworkConfigEvent.Type type;
            switch (event.type()) {
                case INSERT:
                    type = CONFIG_ADDED;
                    break;
                case UPDATE:
                    type = CONFIG_UPDATED;
                    break;
                case REMOVE:
                default:
                    type = CONFIG_REMOVED;
                    break;
            }
            ConfigFactory factory = factoriesByConfig.get(event.key().configClass);
            if (factory != null) {
                notifyDelegate(new NetworkConfigEvent(type, event.key().subject,
                                                      factory.configClass()));
            }
        }
    }
}
