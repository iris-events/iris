package id.global.iris.plugin.model.generator.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathResolver {

    public Path getWorkingDirectory() {
        return Paths.get(StringConstants.MODELS);
    }

    public Path getSourceDirectory() {
        return createPath(getWorkingDirectory(), "src", "main", "java");
    }

    public Path getSchemasDirectory() {
        return createPath(getWorkingDirectory(), StringConstants.SCHEMAS);
    }

    public Path getSchemaPayloadsDirectory() {
        return createPath(getWorkingDirectory(), StringConstants.SCHEMAS, StringConstants.PAYLOAD);
    }

    public Path resolveAsyncApiFilePath(String asyncApiDirectory, String asyncApiFilename) {
        String[] pathSpliced = asyncApiDirectory.split(StringConstants.COMMA);
        return Paths.get("", pathSpliced).resolve(asyncApiFilename);
    }

    private Path createPath(Path basePath, final String... paths) {
        return Paths.get(basePath.toString(), paths);
    }

}
