package io.smallrye.asyncapi.mavenplugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.JarIndexer;
import org.jboss.jandex.Result;

public class IndexCreator {
    public static final String GLOBALID_COMMON_ARTIFACT = "id.global.common:globalid-common";

    private final MavenProject mavenProject;
    private final Log log;
    private final boolean scanDependenciesDisable;
    private final File classesDir;
    private final List<String> includeDependenciesScopes;
    private final List<String> includeDependenciesTypes;

    public IndexCreator(MavenProject mavenProject, Log log, boolean scanDependenciesDisable,
            File classesDir, List<String> includeDependenciesScopes, List<String> includeDependenciesTypes) {
        this.mavenProject = mavenProject;
        this.log = log;
        this.scanDependenciesDisable = scanDependenciesDisable;
        this.classesDir = classesDir;
        this.includeDependenciesScopes = includeDependenciesScopes;
        this.includeDependenciesTypes = includeDependenciesTypes;
    }

    public IndexView createIndex() throws MojoExecutionException {
        List<IndexView> indexes = new ArrayList<>();
        IndexView moduleIndex;
        try {
            moduleIndex = indexModuleClasses();
            indexes.add(moduleIndex);

            var artifactMap = mavenProject.getArtifactMap();

            if (artifactMap.containsKey(GLOBALID_COMMON_ARTIFACT)) {
                Artifact globalIdCommon = (Artifact) artifactMap.get(GLOBALID_COMMON_ARTIFACT);

                Result globalIdCommonResult = JarIndexer.createJarIndex(globalIdCommon.getFile(), new Indexer(),
                        false, false, false);
                indexes.add(globalIdCommonResult.getIndex());
                log.info(String.format("Added %s to index.", GLOBALID_COMMON_ARTIFACT));
            } else {
                log.warn(
                        String.format(
                                "Project doesn't contain %s among it's artifacts. AMQP annotations will not be available to the Jandex scanner",
                                GLOBALID_COMMON_ARTIFACT));
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't compute index", e);
        }
        // TODO we need to add javax.* annotations to index somewhere here!
        if (!scanDependenciesDisable) {
            indexes.add(moduleIndex);
            for (Object a : mavenProject.getArtifacts()) {
                Artifact artifact = (Artifact) a;
                if (includeDependenciesScopes.contains(artifact.getScope())
                        && includeDependenciesTypes.contains(artifact.getType())) {
                    try {
                        Result result = JarIndexer.createJarIndex(artifact.getFile(), new Indexer(),
                                false, false, false);
                        indexes.add(result.getIndex());
                    } catch (Exception e) {
                        log.error("Can't compute index of " + artifact.getFile().getAbsolutePath() + ", skipping", e);
                    }
                }
            }
        }
        return CompositeIndex.create(indexes);
    }

    private Index indexModuleClasses() throws IOException {
        Indexer indexer = new Indexer();
        try (Stream<Path> stream = Files.walk(classesDir.toPath())) {

            List<Path> classFiles = stream
                    .filter(path -> path.toString().endsWith(".class"))
                    .collect(Collectors.toList());
            for (Path path : classFiles) {
                indexer.index(Files.newInputStream(path));
            }
        }
        return indexer.complete();
    }
}
