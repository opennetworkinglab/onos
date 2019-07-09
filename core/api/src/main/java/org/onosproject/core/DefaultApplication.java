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
package org.onosproject.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.onosproject.app.ApplicationDescription;
import org.onosproject.security.Permission;

import java.net.URI;
import java.util.Set;
import java.util.Optional;
import java.util.List;
import java.util.Objects;
import java.net.URL;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of network control/management application descriptor.
 */
public final class DefaultApplication implements Application {

    private final ApplicationId appId;
    private final Version version;
    private final String title;
    private final String description;
    private final String category;
    private final String url;
    private final String readme;
    private final byte[] icon;
    private final String origin;
    private final ApplicationRole role;
    private final Set<Permission> permissions;
    private final Optional<URI> featuresRepo;
    private final List<String> features;
    private final List<String> requiredApps;
    private final URL imageUrl;
    /**
     * Default constructor is hidden to prevent calls to new.
     */
    private DefaultApplication() {
        appId = null;
        version = null;
        title = null;
        description = null;
        category = null;
        url = null;
        readme = null;
        icon = null;
        origin = null;
        role = null;
        permissions = null;
        featuresRepo = Optional.empty();
        features = ImmutableList.of();
        requiredApps = ImmutableList.of();
        imageUrl = null;
    }

    /**
     * Creates a new application descriptor using the supplied data.
     *
     * @param appId        application identifier
     * @param version      application version
     * @param title        application title
     * @param description  application description
     * @param origin       origin company
     * @param category     application category
     * @param url          application URL
     * @param readme       application readme
     * @param icon         application icon
     * @param role         application role
     * @param permissions  requested permissions
     * @param featuresRepo optional features repo URI
     * @param features     application features
     * @param requiredApps list of required application names
     * @param imageUrl     url of oar file
     */
    public DefaultApplication(ApplicationId appId, Version version, String title,
                              String description, String origin, String category,
                              String url, String readme, byte[] icon,
                              ApplicationRole role, Set<Permission> permissions,
                              Optional<URI> featuresRepo, List<String> features,
                              List<String> requiredApps, URL imageUrl) {
        this.appId = appId;
        this.version = version;
        this.title = title;
        this.description = description;
        this.origin = origin;
        this.category = category;
        this.url = url;
        this.readme = readme;
        this.icon = icon == null ? new byte[0] : icon.clone();
        this.role = role;
        this.permissions = ImmutableSet.copyOf(permissions);
        this.featuresRepo = featuresRepo;
        this.features = ImmutableList.copyOf(features);
        this.requiredApps = ImmutableList.copyOf(requiredApps);
        this.imageUrl = imageUrl;
    }
    @Override
    public ApplicationId id() {
        return appId;
    }

    @Override
    public Version version() {
        return version;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String category() {
        return category;
    }

    @Override
    public String url() {
        return url;
    }

    @Override
    public String readme() {
        return readme;
    }

    @Override
    public byte[] icon() {
        return icon.clone();
    }

    @Override
    public String origin() {
        return origin;
    }

    @Override
    public ApplicationRole role() {
        return role;
    }

    @Override
    public Set<Permission> permissions() {
        return permissions;
    }

    @Override
    public Optional<URI> featuresRepo() {
        return featuresRepo;
    }

    @Override
    public List<String> features() {
        return features;
    }

    @Override
    public List<String> requiredApps() {
        return requiredApps;
    }

    @Override
    public URL imageUrl() {
        return imageUrl;
    }

    @Override
    public int hashCode() {
        return Objects.hash(appId, version, title, description, origin, category, url,
                            readme, role, permissions, featuresRepo, features, requiredApps);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final DefaultApplication other = (DefaultApplication) obj;
        // TODO: review -- do ALL the fields need to be included?
        // It is debatable whether fields like description, url, and readme,
        //   need to be included in the notion of equivalence.
        return Objects.equals(this.appId, other.appId) &&
                Objects.equals(this.version, other.version) &&
                Objects.equals(this.title, other.title) &&
                Objects.equals(this.description, other.description) &&
                Objects.equals(this.origin, other.origin) &&
                Objects.equals(this.category, other.category) &&
                Objects.equals(this.url, other.url) &&
                Objects.equals(this.readme, other.readme) &&
                Objects.equals(this.role, other.role) &&
                Objects.equals(this.permissions, other.permissions) &&
                Objects.equals(this.featuresRepo, other.featuresRepo) &&
                Objects.equals(this.features, other.features) &&
                Objects.equals(this.requiredApps, other.requiredApps);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("appId", appId)
                .add("version", version)
                .add("title", title)
                .add("description", description)
                .add("origin", origin)
                .add("category", category)
                .add("url", url)
                .add("readme", readme)
                .add("role", role)
                .add("permissions", permissions)
                .add("featuresRepo", featuresRepo)
                .add("features", features)
                .add("requiredApps", requiredApps)
                .add("imageURL", imageUrl)
                .toString();
    }

    /**
     * Returns a default application builder.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new builder as a copy of an existing builder.
     *
     * @param builder existing builder to copy
     * @return new builder
     */
    public static Builder builder(Builder builder) {
        return new Builder(builder);
    }

    /**
     * Creates a new builder as a copy of an existing application.
     *
     * @param application existing application to copy
     * @return new builder
     */
    public static Builder builder(Application application) {
        return new Builder(application);
    }

    /**
     * Creates a new builder as a copy of an existing application description.
     *
     * @param appDesc existing application description
     * @return new builder
     */
    public static Builder builder(ApplicationDescription appDesc) {
        return new Builder(appDesc);
    }


    /**
     * Default application builder.
     */
    public static final class Builder {
        private ApplicationId appId;
        private Version version;
        private String title;
        private String description;
        private String category;
        private String url;
        private String readme;
        private byte[] icon = new byte[0];
        private String origin;
        private ApplicationRole role = ApplicationRole.ADMIN;
        private Set<Permission> permissions = ImmutableSet.of();
        private Optional<URI> featuresRepo = Optional.empty();
        private List<String> features = ImmutableList.of();
        private List<String> requiredApps = ImmutableList.of();
        private URL imageUrl;

        /**
         * Default constructor for the builder.
         */
        public Builder() {}

        /**
         * Updates the builder to be a copy of an existing builder.
         *
         * @param builder existing builder to copy
         */
        public Builder(Builder builder) {
            this.appId = builder.appId;
            this.version = builder.version;
            this.title = builder.title;
            this.description = builder.description;
            this.category = builder.category;
            this.url = builder.url;
            this.readme = builder.readme;
            this.icon = builder.icon;
            this.origin = builder.origin;
            this.role = builder.role;
            this.permissions = builder.permissions;
            this.featuresRepo = builder.featuresRepo;
            this.features = builder.features;
            this.requiredApps = builder.requiredApps;
        }

        /**
         * Updates the builder to be a copy of an existing application.
         *
         * @param application existing application to copy
         */
        public Builder(Application application) {
            this.appId = application.id();
            this.version = application.version();
            this.title = application.title();
            this.description = application.description();
            this.category = application.category();
            this.url = application.url();
            this.readme = application.readme();
            this.icon = application.icon();
            this.origin = application.origin();
            this.role = application.role();
            this.permissions = application.permissions();
            this.featuresRepo = application.featuresRepo();
            this.features = application.features();
            this.requiredApps = application.requiredApps();
        }

        /**
         * Updates the builder to be a copy of an existing application description.
         *
         * @param appDesc existing application description
         */
        public Builder(ApplicationDescription appDesc) {
            this.version = appDesc.version();
            this.title = appDesc.title();
            this.description = appDesc.description();
            this.category = appDesc.category();
            this.url = appDesc.url();
            this.readme = appDesc.readme();
            this.icon = appDesc.icon();
            this.origin = appDesc.origin();
            this.role = appDesc.role();
            this.permissions = appDesc.permissions();
            this.featuresRepo = appDesc.featuresRepo();
            this.features = appDesc.features();
            this.requiredApps = appDesc.requiredApps();
        }

        /**
         * Adds an application id.
         *
         * @param appId application id
         * @return builder
         */
        public Builder withAppId(ApplicationId appId) {
            this.appId = appId;
            return this;
        }

        /**
         * Adds a version string.
         *
         * @param version version string
         * @return builder
         */
        public Builder withVersion(Version version) {
            this.version = version;
            return this;
        }

        /**
         * Adds a title string.
         *
         * @param title title string
         * @return builder
         */
        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        /**
         * Adds a description string.
         *
         * @param description description string
         * @return builder
         */
        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        /**
         * Adds a category string.
         *
         * @param category category string
         * @return builder
         */
        public Builder withCategory(String category) {
            this.category = category;
            return this;
        }

        /**
         * Adds a URL string.
         *
         * @param url url string
         * @return builder
         */
        public Builder withUrl(String url) {
            this.url = url;
            return this;
        }

        /**
         * Adds a readme string.
         *
         * @param readme readme string
         * @return builder
         */
        public Builder withReadme(String readme) {
            this.readme = readme;
            return this;
        }

        /**
         * Adds an icon.
         *
         * @param icon icon data
         * @return builder
         */
        public Builder withIcon(byte[] icon) {
            this.icon = icon;
            return this;
        }

        /**
         * Adds an origin string.
         *
         * @param origin origin string
         * @return builder
         */
        public Builder withOrigin(String origin) {
            this.origin = origin;
            return this;
        }

        /**
         * Adds an application role.
         *
         * @param role application role
         * @return builder
         */
        public Builder withRole(ApplicationRole role) {
            this.role = role;
            return this;
        }

        /**
         * Adds a permissions set.
         *
         * @param permissions permissions set
         * @return builder
         */
        public Builder withPermissions(Set<Permission> permissions) {
            this.permissions = permissions;
            return this;
        }

        /**
         * Adds a URI for a features repository.
         *
         * @param featuresRepo Optional URI for a features repository
         * @return builder
         */
        public Builder withFeaturesRepo(Optional<URI> featuresRepo) {
            this.featuresRepo = featuresRepo;
            return this;
        }

        /**
         * Adds a features list.
         *
         * @param features features list
         * @return builder
         */
        public Builder withFeatures(List<String> features) {
            this.features = features;
            return this;
        }

        /**
         * Adds a list of required applications.
         *
         * @param requiredApps List of name strings of required applications
         * @return builder
         */
        public Builder withRequiredApps(List<String> requiredApps) {
            this.requiredApps = requiredApps;
            return this;
        }

        /**
         * Adds a Binary Image URL.
         *
         * @param imageUrl url of oar file
         * @return builder
         */
        public Builder withImageUrl(URL imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        /**
         * Builds a default application object from the gathered parameters.
         *
         * @return new default application
         */
        public DefaultApplication build() {
            checkNotNull(appId, "ID cannot be null");
            checkNotNull(version, "Version cannot be null");
            checkNotNull(title, "Title cannot be null");
            checkNotNull(description, "Description cannot be null");
            checkNotNull(origin, "Origin cannot be null");
            checkNotNull(category, "Category cannot be null");
            checkNotNull(readme, "Readme cannot be null");
            checkNotNull(role, "Role cannot be null");
            checkNotNull(permissions, "Permissions cannot be null");
            checkNotNull(featuresRepo, "Features repo cannot be null");
            checkNotNull(features, "Features cannot be null");
            checkNotNull(requiredApps, "Required apps cannot be null");
            checkArgument(!features.isEmpty(), "There must be at least one feature");

            return new DefaultApplication(appId, version, title,
                                          description, origin, category,
                                          url, readme, icon,
                                          role, permissions,
                                          featuresRepo, features,
                                          requiredApps, imageUrl);
        }
    }
}