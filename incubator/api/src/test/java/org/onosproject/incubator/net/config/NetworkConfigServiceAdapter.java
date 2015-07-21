package org.onosproject.incubator.net.config;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Set;

/**
 * Test adapter for network configuration service.
 */
public class NetworkConfigServiceAdapter implements NetworkConfigService {
    @Override
    public Set<Class> getSubjectClasses() {
        return null;
    }

    @Override
    public SubjectFactory getSubjectFactory(String subjectKey) {
        return null;
    }

    @Override
    public SubjectFactory getSubjectFactory(Class subjectClass) {
        return null;
    }

    @Override
    public Class<? extends Config> getConfigClass(String subjectKey, String configKey) {
        return null;
    }

    @Override
    public <S> Set<S> getSubjects(Class<S> subjectClass) {
        return null;
    }

    @Override
    public <S, C extends Config<S>> Set<S> getSubjects(Class<S> subjectClass, Class<C> configClass) {
        return null;
    }

    @Override
    public <S> Set<? extends Config<S>> getConfigs(S subject) {
        return null;
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
    public <S, C extends Config<S>> C applyConfig(S subject, Class<C> configClass, ObjectNode json) {
        return null;
    }

    @Override
    public <S, C extends Config<S>> void removeConfig(S subject, Class<C> configClass) {

    }

    @Override
    public void addListener(NetworkConfigListener listener) {

    }

    @Override
    public void removeListener(NetworkConfigListener listener) {

    }
}
