package io.smallrye.asyncapi.mavenplugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.JarIndexer;
import org.jboss.jandex.Result;

public class IndexCreator {

    private final MavenProject mavenProject;
    private final Log log;
    private final boolean scanDependencies;
    private final File classesDir;
    private final List<String> includeDependenciesScopes;
    private final List<String> includeDependenciesTypes;
    private final List<String> annotationsArtifacts;

    public IndexCreator(MavenProject mavenProject, Log log, boolean scanDependencies,
            File classesDir, List<String> includeDependenciesScopes, List<String> includeDependenciesTypes,
            List<String> annotationsArtifacts) {
        this.mavenProject = mavenProject;
        this.log = log;
        this.scanDependencies = scanDependencies;
        this.classesDir = classesDir;
        this.includeDependenciesScopes = includeDependenciesScopes;
        this.includeDependenciesTypes = includeDependenciesTypes;
        this.annotationsArtifacts = annotationsArtifacts;
    }

    public IndexView createIndex() throws MojoExecutionException {
        final var moduleIndex = getModuleClassIndex();
        final var annotationsArtifactIndexes = getAnnotationsArtifactIndexes();
        final var dependenciesIndexes = getDependenciesIndexes();

        final List<IndexView> indexes = new ArrayList<>();
        indexes.add(moduleIndex);
        indexes.addAll(annotationsArtifactIndexes);
        indexes.addAll(dependenciesIndexes);

        return CompositeIndex.create(indexes);
    }

    private IndexView getModuleClassIndex() throws MojoExecutionException {
        Indexer indexer = new Indexer();
        try (Stream<Path> pathStream = Files.walk(classesDir.toPath())) {
            final var classPaths = pathStream
                    .filter(path -> path.toString().endsWith(".class"))
                    .collect(Collectors.toList());
            for (Path path : classPaths) {
                indexer.index(Files.newInputStream(path));
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't compute index", e);
        }
        return indexer.complete();
    }

    private List<IndexView> getAnnotationsArtifactIndexes() {
        final var artifactMap = mavenProject.getArtifactMap();
        return annotationsArtifacts.stream()
                .map(artifactName -> (Artifact) artifactMap.get(artifactName))
                .map(this::createAnnotationArtifactIndex)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private List<IndexView> getDependenciesIndexes() {
        if (!scanDependencies) {
            return Collections.emptyList();
        }

        final Set<?> artifacts = mavenProject.getArtifacts();
        return artifacts.stream()
                .map(artifactObject -> (Artifact) artifactObject)
                .filter(artifact -> includeDependenciesScopes.contains(artifact.getScope()))
                .filter(artifact -> includeDependenciesTypes.contains(artifact.getType()))
                .map(this::createDependencyArtifactIndex)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

    }

    private Optional<IndexView> createAnnotationArtifactIndex(final Artifact artifact) {
        try {
            final var result = getJarIndex(artifact);
            log.info(String.format("Adding %s:%s to index.", artifact.getGroupId(), artifact.getArtifactId()));
            return Optional.of(result.getIndex());
        } catch (Exception e) {
            final var message = String.format(
                    "Project doesn't contain %s among it's artifacts. AMQP annotations will not be available to the Jandex scanner",
                    artifact);
            log.warn(message);
            return Optional.empty();
        }
    }

    private Optional<IndexView> createDependencyArtifactIndex(final Artifact artifact) {
        try {
            final var result = getJarIndex(artifact);
            return Optional.of(result.getIndex());
        } catch (Exception e) {
            log.error("Can't compute index of " + artifact.getFile().getAbsolutePath() + ", skipping", e);
            return Optional.empty();
        }
    }

    private Result getJarIndex(final Artifact artifact) throws IOException {
        return JarIndexer.createJarIndex(artifact.getFile(), new Indexer(),
                false, false, false);
    }
}
