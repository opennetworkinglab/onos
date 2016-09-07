/*
 * Copyright 2015-present Open Networking Laboratory
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
import com.fasterxml.jackson.databind.node.NullNode;
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
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.InvalidConfigException;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigStore;
import org.onosproject.net.config.NetworkConfigStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
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

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_ADDED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_REGISTERED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_REMOVED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_UNREGISTERED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_UPDATED;

/**
 * Implementation of a distributed network configuration store.
 */
@Component(immediate = true)
@Service
public class DistributedNetworkConfigStore
        extends AbstractStore<NetworkConfigEvent, NetworkConfigStoreDelegate>
        implements NetworkConfigStore {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String INVALID_CONFIG_JSON =
            "JSON node does not contain valid configuration";
    private static final String INVALID_JSON_LIST =
            "JSON node is not a list for list type config";
    private static final String INVALID_JSON_OBJECT =
            "JSON node is not an object for object type config";

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
                          LongNode.class, DoubleNode.class, ShortNode.class, IntNode.class,
                          NullNode.class);

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
        processPendingConfigs(configFactory);
        notifyDelegate(new NetworkConfigEvent(CONFIG_REGISTERED, configFactory.configKey(),
                                              configFactory.configClass()));
    }

    // Sweep through any pending configurations, validate them and then prune them.
    private void processPendingConfigs(ConfigFactory configFactory) {
        ImmutableSet.copyOf(configs.keySet()).forEach(k -> {
            if (Objects.equals(k.configKey, configFactory.configKey()) &&
                    isAssignableFrom(configFactory, k)) {
                // Prune whether valid or not
                Versioned<JsonNode> versioned = configs.remove(k);
                // Allow for the value to be processed by another node already
                if (versioned != null) {
                    validateConfig(k, configFactory, versioned.value());
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private boolean isAssignableFrom(ConfigFactory configFactory, ConfigKey k) {
        return configFactory.subjectFactory().subjectClass().isAssignableFrom(k.subject.getClass());
    }

    @SuppressWarnings("unchecked")
    private void validateConfig(ConfigKey key, ConfigFactory configFactory, JsonNode json) {
        Object subject;
        if (key.subject instanceof String) {
            subject = configFactory.subjectFactory().createSubject((String) key.subject);
        } else {
            subject = key.subject;
        }
        Config config = createConfig(subject, configFactory.configClass(), json);
        try {
            checkArgument(config.isValid(), INVALID_CONFIG_JSON);
            configs.putAndGet(key(subject, configFactory.configClass()), json);
        } catch (Exception e) {
            log.warn("Failed to validate pending {} configuration for {}: {}",
                     key.configKey, key.subject, json);
        }
    }

    @Override
    public void removeConfigFactory(ConfigFactory configFactory) {
        factoriesByConfig.remove(configFactory.configClass().getName());
        processExistingConfigs(configFactory);
        notifyDelegate(new NetworkConfigEvent(CONFIG_UNREGISTERED, configFactory.configKey(),
                                              configFactory.configClass()));
    }

    // Sweep through any configurations for the config factory, set back to pending state.
    private void processExistingConfigs(ConfigFactory configFactory) {
        ImmutableSet.copyOf(configs.keySet()).forEach(k -> {
            if (Objects.equals(configFactory.configClass().getName(), k.configClass)) {
                Versioned<JsonNode> remove = configs.remove(k);
                if (remove != null) {
                    JsonNode json = remove.value();
                    configs.put(key(k.subject, configFactory.configKey()), json);
                    log.debug("Set config pending: {}, {}", k.subject, k.configClass);
                }
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S, C extends Config<S>> ConfigFactory<S, C> getConfigFactory(Class<C> configClass) {
        return factoriesByConfig.get(configClass.getName());
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
            if (subjectClass.isInstance(k.subject) && Objects.equals(cName, k.configClass)) {
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
            if (Objects.equals(subject, k.subject) && k.configClass != null && delegate != null) {
                ConfigFactory<S, ? extends Config<S>> configFactory = factoriesByConfig.get(k.configClass);
                if (configFactory == null) {
                    log.warn("Found config but no config factory: subject={}, configClass={}",
                             subject, k.configClass);
                } else {
                    builder.add(configFactory.configClass());
                }
            }
        });
        return builder.build();
    }

    @Override
    public <S, T extends Config<S>> T getConfig(S subject, Class<T> configClass) {
        Versioned<JsonNode> json = configs.get(key(subject, configClass));
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
        // Create the configuration and validate it.
        C config = createConfig(subject, configClass, json);

        try {
            checkArgument(config.isValid(), INVALID_CONFIG_JSON);
        } catch (RuntimeException e) {
            ConfigFactory<S, C> configFactory = getConfigFactory(configClass);
            String subjectKey = configFactory.subjectFactory().subjectClassKey();
            String subjectString = configFactory.subjectFactory().subjectKey(config.subject());
            String configKey = config.key();

            throw new InvalidConfigException(subjectKey, subjectString, configKey, e);
        }

        // Insert the validated configuration and get it back.
        Versioned<JsonNode> versioned = configs.putAndGet(key(subject, configClass), json);

        // Re-create the config if for some reason what we attempted to put
        // was supplanted by someone else already.
        return versioned.value() == json ? config : createConfig(subject, configClass, versioned.value());
    }

    @Override
    public <S> void queueConfig(S subject, String configKey, JsonNode json) {
        configs.put(key(subject, configKey), json);
    }

    @Override
    public <S, C extends Config<S>> void clearConfig(S subject, Class<C> configClass) {
        configs.remove(key(subject, configClass));
    }

    @Override
    public <S> void clearQueuedConfig(S subject, String configKey) {
        configs.remove(key(subject, configKey));
    }

    @Override
    public <S> void clearConfig(S subject) {
        ImmutableSet.copyOf(configs.keySet()).forEach(k -> {
            if (Objects.equals(subject, k.subject) && delegate != null) {
                configs.remove(k);
            }
        });
    }

    @Override
    public <S> void clearConfig() {
        ImmutableSet.copyOf(configs.keySet()).forEach(k -> {
            if (delegate != null) {
                configs.remove(k);
            }
        });
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
    private <S, C extends Config<S>> C createConfig(S subject, Class<C> configClass,
                                                    JsonNode json) {
        return createConfig(subject, configClass, json, false);
    }

    /**
     * Produces a config from the specified subject, config class and raw JSON.
     *
     * The config can optionally be detached, which means it does not contain a
     * reference to an apply delegate. This means a detached config can not be
     * applied. This should be used only for passing the config object in the
     * NetworkConfigEvent.
     *
     * @param subject     config subject
     * @param configClass config class
     * @param json        raw JSON data
     * @param detached    whether the config should be detached, that is, should
     *                    be created without setting an apply delegate.
     * @return config object or null of no factory found or if the specified
     * JSON is null
     */
    @SuppressWarnings("unchecked")
    private <S, C extends Config<S>> C createConfig(S subject, Class<C> configClass,
                                                    JsonNode json, boolean detached) {
        if (json != null) {
            ConfigFactory<S, C> factory = factoriesByConfig.get(configClass.getName());
            if (factory != null) {
                validateJsonType(json, factory);
                C config = factory.createConfig();
                config.init(subject, factory.configKey(), json, mapper,
                        detached ? null : applyDelegate);
                return config;
            }
        }
        return null;
    }

    /**
     * Validates that the type of the JSON node is appropriate for the type of
     * configuration. A list type configuration must be created with an
     * ArrayNode, and an object type configuration must be created with an
     * ObjectNode.
     *
     * @param json JSON node to check
     * @param factory config factory of configuration
     * @param <S> subject
     * @param <C> configuration
     * @return true if the JSON node type is appropriate for the configuration
     */
    private <S, C extends Config<S>> boolean validateJsonType(JsonNode json,
                                                              ConfigFactory<S, C> factory) {
        if (factory.isList() && !(json instanceof ArrayNode)) {
            throw new IllegalArgumentException(INVALID_JSON_LIST);
        }
        if (!factory.isList() && !(json instanceof ObjectNode)) {
            throw new IllegalArgumentException(INVALID_JSON_OBJECT);
        }

        return true;
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

    // Produces a key for uniquely tracking a subject config.
    private static ConfigKey key(Object subject, String configKey) {
        return new ConfigKey(subject, configKey);
    }

    // Auxiliary key to track subject configurations.
    // Keys with non-null configKey are pending configurations.
    private static final class ConfigKey {
        final Object subject;
        final String configKey;
        final String configClass;

        // Create a key for pending configuration class
        private ConfigKey(Object subject, String configKey) {
            this.subject = subject;
            this.configKey = configKey;
            this.configClass = null;
        }

        // Create a key for registered class configuration
        private ConfigKey(Object subject, Class<?> configClass) {
            this.subject = subject;
            this.configKey = null;
            this.configClass = configClass.getName();
        }

        @Override
        public int hashCode() {
            return Objects.hash(subject, configKey, configClass);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ConfigKey) {
                final ConfigKey other = (ConfigKey) obj;
                return Objects.equals(this.subject, other.subject)
                        && Objects.equals(this.configKey, other.configKey)
                        && Objects.equals(this.configClass, other.configClass);
            }
            return false;
        }
    }

    private class InternalMapListener implements MapEventListener<ConfigKey, JsonNode> {
        @Override
        public void event(MapEvent<ConfigKey, JsonNode> event) {
            // Do not delegate pending configs.
            if (event.key().configClass == null) {
                return;
            }

            ConfigFactory factory = factoriesByConfig.get(event.key().configClass);
            if (factory != null) {
                Object subject = event.key().subject;
                Class configClass = factory.configClass();
                Versioned<JsonNode> newValue = event.newValue();
                Versioned<JsonNode> oldValue = event.oldValue();

                Config config = (newValue != null) ?
                                createConfig(subject, configClass, newValue.value(), true) :
                                null;
                Config prevConfig = (oldValue != null) ?
                                    createConfig(subject, configClass, oldValue.value(), true) :
                                    null;

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
                notifyDelegate(new NetworkConfigEvent(type, event.key().subject,
                        config, prevConfig, factory.configClass()));
            }
        }
    }
}
