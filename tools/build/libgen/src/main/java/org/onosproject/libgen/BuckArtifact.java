/*
 * Copyright 2016-present Open Networking Foundation
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

import org.eclipse.aether.artifact.Artifact;

/**
 * Representation of a remote artifact for Buck.
 */
public abstract class BuckArtifact {

    private final String name;
    private final String sha;
    private final boolean osgiReady;
    private final boolean generateForBazel;

    public static BuckArtifact getArtifact(String name, Artifact artifact, String sha, String repo,
                                           boolean osgiReady, boolean generateForBazel) {
        return new MavenArtifact(name, artifact, sha, repo, osgiReady, generateForBazel);
    }
    public static BuckArtifact getArtifact(String name, String url, String sha, String mavenCoords,
                                           boolean osgiReady, boolean generateForBazel) {
        return new HTTPArtifact(name, url, sha, mavenCoords, osgiReady, generateForBazel);
    }
    public static BuckArtifact getArtifact(String name, String url, String sha, boolean generateForBazel) {
        return new HTTPArtifact(name, url, sha, null, true, generateForBazel);
    }

    public BuckArtifact(String name, String sha, boolean osgiReady, boolean generateForBazel) {
        this.name = name;
        this.sha = sha;
        this.osgiReady = osgiReady;
        this.generateForBazel = generateForBazel;
    }

    public String name() {
        if (!generateForBazel) {
            return name;
        } else {
            return name.replaceAll("[.-]", "_");
        }
    }

    abstract String fileName();

    abstract String url();

    abstract String url(boolean withClassifier);

    private String jarTarget() {
        return name != null ? name() : fileName();
    }

    private boolean isPublic() {
        return name != null;
    }

    boolean isOsgiReady() {
        return osgiReady;
    }

    boolean isGenerateForBazel() {
        return generateForBazel;
    }


    String mavenCoords() {
        return null;
    }

    String bazelExport() {
        return "@" + jarTarget() + "//jar";
    }

    private boolean isJar() {
        return fileName().endsWith(".jar");
    }

    private boolean isHttp() {
        return url().startsWith("http");
    }

    String getBazelJavaLibraryFragment() {
        if (isJar()) {
            String format =
                    "\n    native.java_library(\n" +
                            "        name = \"%s\",\n" +
                            "        visibility = [\"//visibility:public\"],\n" +
                            "        exports = [\"@%s//jar\"],\n" +
                            "    )\n";
            return String.format(format, jarTarget(), jarTarget());
        }
        return "";
    }

    private String extractRepo() {
        // This is a hack because the code above us already got rid of the maven repo
        // info for artifacts
        String url = url();
        if (url.startsWith("http")) {
            return url.substring(0, url.indexOf(fileName()) - mavenCoords().length() - 1);
        } else {
            return "";
        }
    }

    String getBazelMavenJarFragment() {
        if (isJar() && mavenCoords() != null) {
            String repo = extractRepo();
            String repoAttribute = "";
            if (!"".equals(repo)) {
                repoAttribute = "        repository = \"" + repo + "\",\n";
            }
            String format =
                    "\n    native.maven_jar(\n" +
                            "        name = \"%s\",\n" +
                            "        artifact = \"%s\",\n" +
                            "        sha1 = \"%s\",\n" +
                            "%s" +
                            "    )\n";
            return String.format(format, jarTarget(), mavenCoords(), sha, repoAttribute);
        } else {
            String format =
                    "\n    native.http_file(\n" +
                            "        name = \"%s\",\n" +
                            "        url = \"%s\",\n" +
                            "        sha256 = \"%s\",\n" +
                            "    )\n";
            return String.format(format, jarTarget(), url(), sha);
        }
    }

    public String getBuckFragment() {
        String visibility = isPublic() ? "[ 'PUBLIC' ]" : "[]";

        String output = (isJar() ? "remote_jar" : "remote_file") + " (\n" +
                "  name = '%s',\n" + // jar target
                "  out = '%s',\n" + // jar file name
                "  url = '%s',\n" + // maven url
                "  sha1 = '%s',\n" + // artifact sha
                ( isJar() && mavenCoords() != null ?
                "  maven_coords = '"+ mavenCoords()+"',\n" : "" ) +
                "  visibility = %s,\n" +
                ")\n\n";

        return String.format(output, jarTarget(), fileName(), url(), sha, visibility);
    }

    private static class HTTPArtifact extends BuckArtifact {
        private final String url;
        private final String mavenCoords;

        public HTTPArtifact(String name, String url, String sha,
                            String mavenCoords, boolean osgiReady, boolean generateForBazel) {
            super(name, sha, osgiReady, generateForBazel);
            this.url = url;
            this.mavenCoords = mavenCoords;
        }

        @Override
        String fileName() {
            String[] parts = url.split("/");
            return parts[parts.length - 1];
        }

        @Override
        String mavenCoords() {
            return mavenCoords;
        }

        @Override
        String url() {
            return url;
        }

        @Override
        String url(boolean withClassifier) {
            return url;
        }
    }

    private static class MavenArtifact extends BuckArtifact {
        private final Artifact artifact;
        private final String repo;

        private MavenArtifact(String name, Artifact artifact, String sha,
                              String repo, boolean osgiReady, boolean generateForBazel) {
            super(name, sha, osgiReady, generateForBazel);
            this.artifact = artifact;
            this.repo = repo;
        }

        @Override
        String url() {
            //mvn:[repo:]groupId:artifactId:[extension:[classifier:]]:version
            StringBuilder mvnUrl = new StringBuilder("mvn:");
            if (repo != null && repo.length() > 0) {
                mvnUrl.append(repo).append(':');
            }
            mvnUrl.append(artifact.getGroupId()).append(':')
                  .append(artifact.getArtifactId()).append(':')
                  .append(artifact.getExtension()).append(':');
            if (artifact.getClassifier() != null && artifact.getClassifier().length() > 0) {
                mvnUrl.append(artifact.getClassifier()).append(':');
            }
            mvnUrl.append(artifact.getVersion());
            return mvnUrl.toString();
        }

        @Override
        String url(boolean withClassifier) {
            String url = url();
            if (withClassifier && !isOsgiReady()) {
                int i = url.lastIndexOf(':');
                if (i > 0) {
                    url = url.substring(0, i) + ":NON-OSGI" + url.substring(i);
                }
            }
            return url;
        }

        //FIXME get sources jars

        @Override
        String mavenCoords() {
            String classifer = artifact.getClassifier();
            if (!isOsgiReady() && !isGenerateForBazel()) {
                classifer = "NON-OSGI" + classifer;
            }

            if ("jar".equals(artifact.getExtension().toLowerCase()) &&
                    classifer.length() == 0) {
                // shorter form
                return String.format("%s:%s:%s",
                                     artifact.getGroupId(),
                                     artifact.getArtifactId(),
                                     artifact.getVersion());
            }
            return String.format("%s:%s:%s:%s:%s",
                                 artifact.getGroupId(),
                                 artifact.getArtifactId(),
                                 artifact.getExtension(),
                                 classifer,
                                 artifact.getVersion());
        }

        @Override
        String fileName() {
            return String.format("%s-%s.%s",
                                 artifact.getArtifactId(),
                                 artifact.getVersion(),
                                 artifact.getExtension());
        }
    }
}
