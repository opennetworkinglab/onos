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
package org.onosproject.net.config.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.NetworkConfigStore;
import org.onosproject.net.config.NetworkConfigStoreDelegate;
import org.onosproject.net.config.SubjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.*;

/**
 * Implementation of the network configuration subsystem.
 */
@Component(immediate = true)
@Service
public class NetworkConfigManager
        extends AbstractListenerManager<NetworkConfigEvent, NetworkConfigListener>
        implements NetworkConfigRegistry, NetworkConfigService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String NULL_FACTORY_MSG = "Factory cannot be null";
    private static final String NULL_SCLASS_MSG = "Subject class cannot be null";
    private static final String NULL_SCKEY_MSG = "Subject class key cannot be null";
    private static final String NULL_CCLASS_MSG = "Config class cannot be null";
    private static final String NULL_CKEY_MSG = "Config key cannot be null";
    private static final String NULL_SUBJECT_MSG = "Subject cannot be null";
    private static final String NULL_JSON_MSG = "JSON cannot be null";

    // Inventory of configuration factories
    private final Map<ConfigKey, ConfigFactory> factories = Maps.newConcurrentMap();

    // Secondary indices to retrieve subject and config classes by keys
    private final Map<String, SubjectFactory> subjectClasses = Maps.newConcurrentMap();
    private final Map<Class, SubjectFactory> subjectClassKeys = Maps.newConcurrentMap();
    private final Map<ConfigIdentifier, Class<? extends Config>> configClasses = Maps.newConcurrentMap();

    private final NetworkConfigStoreDelegate storeDelegate = new InternalStoreDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigStore store;


    @Activate
    public void activate() {
        eventDispatcher.addSink(NetworkConfigEvent.class, listenerRegistry);
        store.setDelegate(storeDelegate);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(NetworkConfigEvent.class);
        store.unsetDelegate(storeDelegate);
        log.info("Stopped");
    }


    @Override
    @SuppressWarnings("unchecked")
    public void registerConfigFactory(ConfigFactory configFactory) {
        checkNotNull(configFactory, NULL_FACTORY_MSG);
        factories.put(key(configFactory), configFactory);
        configClasses.put(identifier(configFactory), configFactory.configClass());

        SubjectFactory subjectFactory = configFactory.subjectFactory();
        subjectClasses.putIfAbsent(subjectFactory.subjectClassKey(), subjectFactory);
        subjectClassKeys.putIfAbsent(subjectFactory.subjectClass(), subjectFactory);

        store.addConfigFactory(configFactory);
    }

    @Override
    public void unregisterConfigFactory(ConfigFactory configFactory) {
        checkNotNull(configFactory, NULL_FACTORY_MSG);
        factories.remove(key(configFactory));
        configClasses.remove(identifier(configFactory));

        // Note that we are deliberately not removing subject factory key bindings.
        store.removeConfigFactory(configFactory);
    }

    @Override
    public Set<ConfigFactory> getConfigFactories() {
        return ImmutableSet.copyOf(factories.values());
    }


    @Override
    @SuppressWarnings("unchecked")
    public <S, C extends Config<S>> Set<ConfigFactory<S, C>> getConfigFactories(Class<S> subjectClass) {
        ImmutableSet.Builder<ConfigFactory<S, C>> builder = ImmutableSet.builder();
        factories.forEach((key, factory) -> {
            if (factory.subjectFactory().subjectClass().equals(subjectClass)) {
                builder.add(factory);
            }
        });
        return builder.build();
    }

    @Override
    public <S, C extends Config<S>> ConfigFactory<S, C> getConfigFactory(Class<C> configClass) {
        checkNotNull(configClass, NULL_CCLASS_MSG);
        return store.getConfigFactory(configClass);
    }


    @Override
    public Set<Class> getSubjectClasses() {
        checkPermission(CONFIG_READ);
        ImmutableSet.Builder<Class> builder = ImmutableSet.builder();
        factories.forEach((k, v) -> builder.add(k.subjectClass));
        return builder.build();
    }

    @Override
    public SubjectFactory getSubjectFactory(String subjectClassKey) {
        checkPermission(CONFIG_READ);
        return subjectClasses.get(subjectClassKey);
    }

    @Override
    public SubjectFactory getSubjectFactory(Class subjectClass) {
        checkPermission(CONFIG_READ);
        return subjectClassKeys.get(subjectClass);
    }

    @Override
    public Class<? extends Config> getConfigClass(String subjectClassKey, String configKey) {
        checkPermission(CONFIG_READ);
        checkNotNull(subjectClassKey, NULL_SCKEY_MSG);
        checkNotNull(configKey, NULL_CKEY_MSG);
        return configClasses.get(new ConfigIdentifier(subjectClassKey, configKey));
    }

    @Override
    public <S> Set<S> getSubjects(Class<S> subjectClass) {
        checkPermission(CONFIG_READ);
        checkNotNull(subjectClass, NULL_SCLASS_MSG);
        return store.getSubjects(subjectClass);
    }

    @Override
    public <S, C extends Config<S>> Set<S> getSubjects(Class<S> subjectClass, Class<C> configClass) {
        checkPermission(CONFIG_READ);
        checkNotNull(subjectClass, NULL_SCLASS_MSG);
        checkNotNull(configClass, NULL_CCLASS_MSG);
        return store.getSubjects(subjectClass, configClass);
    }

    @Override
    public <S> Set<Config<S>> getConfigs(S subject) {
        checkPermission(CONFIG_READ);
        checkNotNull(subject, NULL_SUBJECT_MSG);
        Set<Class<? extends Config<S>>> configClasses = store.getConfigClasses(subject);
        ImmutableSet.Builder<Config<S>> cfg = ImmutableSet.builder();
        configClasses.forEach(cc -> cfg.add(store.getConfig(subject, cc)));
        return cfg.build();
    }

    @Override
    public <S, C extends Config<S>> C getConfig(S subject, Class<C> configClass) {
        checkPermission(CONFIG_READ);
        checkNotNull(subject, NULL_SUBJECT_MSG);
        checkNotNull(configClass, NULL_CCLASS_MSG);
        return store.getConfig(subject, configClass);
    }


    @Override
    public <S, C extends Config<S>> C addConfig(S subject, Class<C> configClass) {
        checkPermission(CONFIG_WRITE);
        checkNotNull(subject, NULL_SUBJECT_MSG);
        checkNotNull(configClass, NULL_CCLASS_MSG);
        return store.createConfig(subject, configClass);
    }

    @Override
    public <S, C extends Config<S>> C applyConfig(S subject, Class<C> configClass, JsonNode json) {
        checkPermission(CONFIG_WRITE);
        checkNotNull(subject, NULL_SUBJECT_MSG);
        checkNotNull(configClass, NULL_CCLASS_MSG);
        checkNotNull(json, NULL_JSON_MSG);
        return store.applyConfig(subject, configClass, json);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S, C extends Config<S>> C applyConfig(String subjectClassKey, S subject,
                                                  String configKey, JsonNode json) {
        checkPermission(CONFIG_WRITE);
        checkNotNull(subjectClassKey, NULL_SCKEY_MSG);
        checkNotNull(subject, NULL_SUBJECT_MSG);
        checkNotNull(configKey, NULL_CKEY_MSG);
        checkNotNull(json, NULL_JSON_MSG);
        Class<? extends Config> configClass = configClasses.get(new ConfigIdentifier(subjectClassKey, configKey));
        if (configClass != null) {
            return store.applyConfig(subject, (Class<C>) configClass, json);
        } else {
            log.info("Configuration \'{}\' queued for subject {}", configKey, subject);
            store.queueConfig(subject, configKey, json);
            return null;
        }
    }

    @Override
    public <S, C extends Config<S>> void removeConfig(S subject, Class<C> configClass) {
        checkPermission(CONFIG_WRITE);
        checkNotNull(subject, NULL_SUBJECT_MSG);
        checkNotNull(configClass, NULL_CCLASS_MSG);
        store.clearConfig(subject, configClass);
    }

    @Override
    public <S> void removeConfig(String subjectClassKey, S subject, String configKey) {
        checkNotNull(subjectClassKey, NULL_SCKEY_MSG);
        checkNotNull(subject, NULL_SUBJECT_MSG);
        checkNotNull(configKey, NULL_CKEY_MSG);
        Class<? extends Config> configClass = configClasses.get(new ConfigIdentifier(subjectClassKey, configKey));
        if (configClass != null) {
            store.clearConfig(subject, configClass);
        } else {
            store.clearQueuedConfig(subject, configKey);
         }
    }

     @Override
     public <S> void removeConfig(S subject) {
        checkPermission(CONFIG_WRITE);
        store.clearConfig(subject);
     }

     @Override
     public <S> void removeConfig() {
         checkPermission(CONFIG_WRITE);
         store.clearConfig();
     }

    // Auxiliary store delegate to receive notification about changes in
    // the network configuration store state - by the store itself.
    private class InternalStoreDelegate implements NetworkConfigStoreDelegate {
        @Override
        public void notify(NetworkConfigEvent event) {
            post(event);
        }
    }


    // Produces a key for uniquely tracking a config factory.
    private static ConfigKey key(ConfigFactory factory) {
        return new ConfigKey(factory.subjectFactory().subjectClass(), factory.configClass());
    }

    // Auxiliary key to track config factories.
    protected static final class ConfigKey {
        final Class subjectClass;
        final Class configClass;

        protected ConfigKey(Class subjectClass, Class configClass) {
            this.subjectClass = subjectClass;
            this.configClass = configClass;
        }

        @Override
        public int hashCode() {
            return Objects.hash(subjectClass, configClass);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ConfigKey) {
                final ConfigKey other = (ConfigKey) obj;
                return Objects.equals(this.subjectClass, other.subjectClass)
                        && Objects.equals(this.configClass, other.configClass);
            }
            return false;
        }
    }

    private static ConfigIdentifier identifier(ConfigFactory factory) {
        return new ConfigIdentifier(factory.subjectFactory().subjectClassKey(), factory.configKey());
    }

    static final class ConfigIdentifier {
        final String subjectClassKey;
        final String configKey;

        protected ConfigIdentifier(String subjectClassKey, String configKey) {
            this.subjectClassKey = subjectClassKey;
            this.configKey = configKey;
        }

        @Override
        public int hashCode() {
            return Objects.hash(subjectClassKey, configKey);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ConfigIdentifier) {
                final ConfigIdentifier other = (ConfigIdentifier) obj;
                return Objects.equals(this.subjectClassKey, other.subjectClassKey)
                        && Objects.equals(this.configKey, other.configKey);
            }
            return false;
        }
    }

}
