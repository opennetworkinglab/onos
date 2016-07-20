/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.libgen;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.version.Version;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import static org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_WARN;
import static org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_ALWAYS;

/**
 * Resolver capable of resolving Maven coordinates to a Maven artifact.
 */
public class AetherResolver {
    private static final String CENTRAL_URL = "http://repo1.maven.org/maven2/";

    private static RepositorySystem system;
    private static RepositorySystemSession session;
    private static final RemoteRepository CENTRAL =
            new RemoteRepository.Builder("central", "default", CENTRAL_URL).build();

    private final String repoUrl;

    static {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class );
        locator.addService(TransporterFactory.class, FileTransporterFactory.class );
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class );

        locator.setErrorHandler( new DefaultServiceLocator.ErrorHandler()
        {
            @Override
            public void serviceCreationFailed( Class<?> type, Class<?> impl, Throwable exception )
            {
                exception.printStackTrace();
            }
        } );

        AetherResolver.system = locator.getService( RepositorySystem.class );

        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        LocalRepository localRepo = new LocalRepository("target/local-repo" );
        session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepo ) );

        //session.setTransferListener( new ConsoleTransferListener() );
        //session.setRepositoryListener( new ConsoleRepositoryListener() );
        AetherResolver.session = session;
    }

    public static BuckArtifact getArtifact(String name, String uri, String repo) {
        return new AetherResolver(repo).build(name, uri);
    }

    private AetherResolver(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    private BuckArtifact build(String name, String uri) {
        uri = uri.replaceFirst("mvn:", "");
        Artifact artifact = new DefaultArtifact(uri);
        String originalVersion = artifact.getVersion();
        try {
            artifact = artifact.setVersion(newestVersion(artifact));
            artifact = resolveArtifact(artifact);
            String sha = getSha(artifact);
            boolean osgiReady = isOsgiReady(artifact);

            if (originalVersion.endsWith("-SNAPSHOT")) {
                String url = String.format("%s/%s/%s/%s/%s-%s.%s",
                                            repoUrl,
                                            artifact.getGroupId().replace('.', '/'),
                                            artifact.getArtifactId(),
                                            originalVersion,
                                            artifact.getArtifactId(),
                                            artifact.getVersion(),
                                            artifact.getExtension());
                String mavenCoords = String.format("%s:%s:%s",
                                                   artifact.getGroupId(),
                                                   artifact.getArtifactId(),
                                                   originalVersion);
                return BuckArtifact.getArtifact(name, url, sha, mavenCoords, osgiReady);
            }
            return BuckArtifact.getArtifact(name, artifact, sha, repoUrl, osgiReady);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Artifact resolveArtifact(Artifact artifact) throws Exception {
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(artifact);
        request.setRepositories(repositories());
        ArtifactResult result = system.resolveArtifact(session, request);
        return result.getArtifact();
    }

    private boolean isOsgiReady(Artifact artifact) throws Exception {
        JarFile jar = new JarFile(artifact.getFile());
        Attributes attrs = jar.getManifest().getMainAttributes();
        return attrs.getValue("Bundle-SymbolicName") != null &&
                attrs.getValue("Bundle-Version") != null;
    }

    private String getSha(Artifact artifact) throws Exception {
        String directory = artifact.getFile().getParent();
        String file = String.format("%s-%s.%s.sha1",
                                    artifact.getArtifactId(),
                                    artifact.getVersion(),
                                    artifact.getExtension());
        String shaPath = Paths.get(directory, file).toString();

        try (Reader reader = new FileReader(shaPath)) {
            return new BufferedReader(reader).readLine().trim().split(" ", 2)[0];
        }
    }

    private String newestVersion(Artifact artifact) throws VersionRangeResolutionException {
        VersionRangeRequest rangeRequest = new VersionRangeRequest();
        rangeRequest.setArtifact(artifact);
        rangeRequest.setRepositories(repositories());

        VersionRangeResult rangeResult = system.resolveVersionRange(session, rangeRequest );

        Version newestVersion = rangeResult.getHighestVersion();

        return newestVersion.toString();
    }

    public List<RemoteRepository> repositories()
    {
        if (repoUrl != null && repoUrl.length() > 0) {
            RepositoryPolicy policy = new RepositoryPolicy(true,
                                                           UPDATE_POLICY_ALWAYS,
                                                           CHECKSUM_POLICY_WARN);
            RemoteRepository repository =
                    new RemoteRepository.Builder("temp", "default", repoUrl)
                            .setSnapshotPolicy(policy).build();
            return Arrays.asList(CENTRAL, repository);
        }

        return Collections.singletonList(CENTRAL);
    }
}
