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
package org.onosproject.app.impl;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesListener;
import org.apache.karaf.features.Repository;

import java.net.URI;
import java.util.EnumSet;
import java.util.Set;

/**
 * Adapter for testing against Apache Karaf feature service.
 */
public class FeaturesServiceAdapter implements org.apache.karaf.features.FeaturesService {
    @Override
    public void validateRepository(URI uri) throws Exception {

    }

    @Override
    public void addRepository(URI uri) throws Exception {

    }

    @Override
    public void addRepository(URI uri, boolean install) throws Exception {

    }

    @Override
    public void removeRepository(URI uri) throws Exception {

    }

    @Override
    public void removeRepository(URI uri, boolean uninstall) throws Exception {

    }

    @Override
    public void restoreRepository(URI uri) throws Exception {

    }

    @Override
    public Repository[] listRepositories() {
        return new Repository[0];
    }

    @Override
    public Repository getRepository(String repoName) {
        return null;
    }

    @Override
    public Repository getRepository(URI uri) {
        return null;
    }

    @Override
    public String getRepositoryName(URI uri) {
        return null;
    }

    @Override
    public void installFeature(String name) throws Exception {

    }

    @Override
    public void installFeature(String name, EnumSet<Option> options) throws Exception {

    }

    @Override
    public void installFeature(String name, String version) throws Exception {

    }

    @Override
    public void installFeature(String name, String version, EnumSet<Option> options) throws Exception {

    }

    @Override
    public void installFeature(Feature f, EnumSet<Option> options) throws Exception {

    }

    @Override
    public void installFeatures(Set<Feature> features, EnumSet<Option> options) throws Exception {

    }

    @Override
    public void uninstallFeature(String name, EnumSet<Option> options) throws Exception {

    }

    @Override
    public void uninstallFeature(String name) throws Exception {

    }

    @Override
    public void uninstallFeature(String name, String version, EnumSet<Option> options) throws Exception {

    }

    @Override
    public void uninstallFeature(String name, String version) throws Exception {

    }

    @Override
    public Feature[] listFeatures() throws Exception {
        return new Feature[0];
    }

    @Override
    public Feature[] listInstalledFeatures() {
        return new Feature[0];
    }

    @Override
    public boolean isInstalled(Feature f) {
        return false;
    }

    @Override
    public Feature[] getFeatures(String name, String version) throws Exception {
        return new Feature[0];
    }

    @Override
    public Feature[] getFeatures(String name) throws Exception {
        return new Feature[0];
    }

    @Override
    public Feature getFeature(String name, String version) throws Exception {
        return null;
    }

    @Override
    public Feature getFeature(String name) throws Exception {
        return null;
    }

    @Override
    public void refreshRepository(URI uri) throws Exception {
    }

    @Override
    public void registerListener(FeaturesListener featuresListener) {
    }

    @Override
    public void unregisterListener(FeaturesListener featuresListener) {
    }
}
