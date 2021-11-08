package id.global.plugin.model.generator.utils;

import static id.global.plugin.model.generator.utils.StringConstants.COMMA;
import static id.global.plugin.model.generator.utils.StringConstants.EMPTY_STRING;
import static id.global.plugin.model.generator.utils.StringConstants.MODELS;
import static id.global.plugin.model.generator.utils.StringConstants.PAYLOAD;
import static id.global.plugin.model.generator.utils.StringConstants.SCHEMAS;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathResolver {

    public Path getRootDirectory() {
        return Paths.get(EMPTY_STRING);
    }

    public Path getWorkingDirectory() {
        return getRootDirectory().resolve(MODELS);
    }

    public Path getSourceDirectory() {
        return createPath(getWorkingDirectory(), "src", "main", "java");
    }

    public Path getSchemasDirectory() {
        return createPath(getWorkingDirectory(), SCHEMAS);
    }

    public Path getSchemaPayloadsDirectory() {
        return createPath(getWorkingDirectory(), SCHEMAS, PAYLOAD);
    }

    public Path resolveAsyncApiFilePath(String asyncApiDirectory, String asyncApiFilename) {
        String[] pathSpliced = asyncApiDirectory.split(COMMA);
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
