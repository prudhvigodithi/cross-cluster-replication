/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */
package org.opensearch.gradle.zipplugin;

import java.util.*;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import java.nio.file.Path;
import org.gradle.api.Task;

public class ZipPublish implements Plugin<Project> {
    private Project project;

    public final static String EXTENSION_NAME = "zipmavensettings";
    public final static String PUBLICATION_NAME = "mavenzip";
    public final static String STAGING_REPO = "zipstaging";
    public final static String MAVEN_ZIP_PUBLISH_TASK = "publish"
        + ZipPublishUtil.capitalize(PUBLICATION_NAME)
        + "PublicationTo"
        + ZipPublishUtil.capitalize(STAGING_REPO)
        + "Repository";
    public final static String MAVEN_ZIP_PUBLISH_POM_TASK = "generatePomFileFor"
        + ZipPublishUtil.capitalize(PUBLICATION_NAME)
        + "Publication";
    public final static String LOCALMAVEN = "publishToMavenLocal";
    public final static String LOCAL_STAGING_REPO_PATH = "/build/local-staging-repo";
    public final static String BUILD_DISTRIBUTIONS_LOCATION = "/build/distributions/";

    private void configMaven() {
        final Path buildDirectory = this.project.getRootDir().toPath();
        this.project.getPluginManager().apply(MavenPublishPlugin.class);
        this.project.getExtensions().configure(PublishingExtension.class, publishing -> {
            publishing.repositories(repositories -> {
                repositories.maven(maven -> {
                    maven.setName(STAGING_REPO);
                    maven.setUrl(buildDirectory.toString() + LOCAL_STAGING_REPO_PATH);
                });
            });
            System.out.println("Starting " + MAVEN_ZIP_PUBLISH_TASK + " task");
            publishing.publications(publications -> {
                publications.create(PUBLICATION_NAME, MavenPublication.class, mavenZip -> {
                    ZipPublishExtension extset = this.project.getExtensions().findByType(ZipPublishExtension.class);
                    // Getting the Zip group from created extension
                    String zipGroup = extset.getZipgroup();
                    String zipArtifact = getProperty("zipArtifact");
                    // Getting the Zip version from gradle property with/without added snapshot and qualifier
                    String zipVersion = getProperty("zipVersion");
                    String version = null;
                    String extraSuffix = null;
                    if (zipVersion != null) {
                        StringTokenizer st = new StringTokenizer(zipVersion);
                        version = st.nextToken("-") + ".0";
                        try {
                            extraSuffix = zipVersion.substring(zipVersion.indexOf("-"));
                        } catch (Exception e) {
                            System.out.println("");
                        }
                    }
                    ;
                    String finalZipVersion = version + extraSuffix;
                    String zipFilePath = BUILD_DISTRIBUTIONS_LOCATION + zipArtifact + "-" + finalZipVersion + ".zip";
                    // -PzipFilePath=/build/distributions/opensearch-job-scheduler-2.0.0.0-alpha1-SNAPSHOT.zip
                    mavenZip.artifact(buildDirectory.toString() + zipFilePath);
                    mavenZip.setGroupId(zipGroup);
                    mavenZip.setArtifactId(zipArtifact);
                    mavenZip.setVersion(finalZipVersion);
                });
            });
        });
    }

    private String getProperty(String name) {
        if (this.project.hasProperty(name)) {
            Object property = this.project.property(name);
            if (property != null) {
                return property.toString();
            }
        }
        return null;
    }

    @Override
    public void apply(Project project) {
        final Path buildDirectory = project.getRootDir().toPath();
        this.project = project;
        project.getExtensions().create(EXTENSION_NAME, ZipPublishExtension.class);
        // Applies the new publication once the plugin is applied
        configMaven();
        Task compileJava = project.getTasks().findByName("compileJava");
        if (compileJava != null) {
            compileJava.setEnabled(false);
        }
        Task sourceJarTask = project.getTasks().findByName("sourcesJar");
        if (sourceJarTask != null) {
            sourceJarTask.setEnabled(false);
        }
        Task javaDocJarTask = project.getTasks().findByName("javadocJar");
        if (javaDocJarTask != null) {
            javaDocJarTask.setEnabled(false);
        }
        project.getGradle().getTaskGraph().whenReady(graph -> {
            if (graph.hasTask(LOCALMAVEN)) {
                project.getTasks().getByName(MAVEN_ZIP_PUBLISH_POM_TASK).setEnabled(false);
            }

        });
    }
}

