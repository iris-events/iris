package id.global.iris.plugin.model.generator.utils;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathResolver {

    public Path getRootDirectory() {
        return Paths.get(StringConstants.EMPTY_STRING);
    }

    public Path getWorkingDirectory() {
        return getRootDirectory().resolve(StringConstants.MODELS);
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
        String path = String.join(FileSystems.getDefault().getSeparator(), pathSpliced);
        return getRootDirectory().resolve(path).resolve(asyncApiFilename);
    }

    private Path createPath(Path basePath, final String... paths) {
        for (String path : paths) {
            basePath = basePath.resolve(path);
        }
        return basePath;
    }

}
