package id.global.plugin.model.generator.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathResolver {

    public Path getRootDirectory() {
        return Paths.get("");
    }

    public Path getWorkingDirectory() {
        return getRootDirectory().resolve("models");
    }

    public Path getSourceDirectory() {
        return createPath(getWorkingDirectory(), "src", "main", "java");
    }

    public Path getSchemasDirectory() {
        return createPath(getWorkingDirectory(), "schemas");
    }

    private Path createPath(Path basePath, final String... paths) {
        for (String path : paths) {
            basePath = basePath.resolve(path);
        }
        return basePath;
    }

}
