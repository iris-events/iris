package id.global.iris.plugin.model.generator.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Stream;

import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import id.global.iris.plugin.model.generator.AmqpGeneratorMojo;

public class FileInteractor {

    private final Log log;

    private final PathResolver pathResolver;

    public FileInteractor(PathResolver pathResolver) {
        this.pathResolver = pathResolver;
        this.log = new DefaultLog(new ConsoleLogger());

    }

    public String readFile(final Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Reading from file failed!", e);
            throw new RuntimeException(e);
        }
    }

    public String readResourceFileContent(final String fileName) {
        var r = AmqpGeneratorMojo.class.getClassLoader().getResource(fileName);
        if (r == null) {
            throw new IllegalArgumentException(String.format("Cannot get input stream for resource file: [%s]", fileName));
        }
        try {
            return readResource(r.toURI());
        } catch (URISyntaxException | IOException | NullPointerException e) {
            log.error("Cannot read resource file content!", e);
            throw new RuntimeException(e);
        }
    }

    public void cleanUpDirectories(final Path tmpFolder) {
        try {
            deleteDirectoryRecursively(tmpFolder);
        } catch (IOException e) {
            log.error("Directory cleanup failed!", e);
            throw new RuntimeException(e);
        }
    }

    private void deleteDirectoryRecursively(final Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk
                    .sorted(Comparator.reverseOrder())
                    .forEach(this::deleteDirectory);
        }
    }

    private void deleteDirectory(final Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            System.err.printf("Unable to delete this path : %s%n%s", path, e);
            throw new RuntimeException(e);
        }
    }

    private void createDirectories(final Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            log.error("Failed to create directories", e);
            throw new RuntimeException(e);
        }
    }

    public void writeFile(final Path path, final String content) {
        try {
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to write file", e);
            throw new RuntimeException(e);
        }
    }

    public String readContentFromWeb(final String contentUrl) {
        try {
            log.info("Reading AsyncApi definition from url: " + contentUrl);
            URL url = new URL(contentUrl);
            String inputLine;
            StringBuilder builder = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
                while ((inputLine = br.readLine()) != null) {
                    builder.append(inputLine);
                }
            }

            return builder.toString();
        } catch (IOException e) {
            log.error("Failed to read content from web", e);
            throw new RuntimeException(e);
        }
    }

    public void initializeDirectories() {
        createDirectories(
                pathResolver.getSourceDirectory());

        createDirectories(
                pathResolver.getSchemasDirectory().resolve(StringConstants.PAYLOAD));
    }

    public static String readResource(URI uri) throws IOException {
        try {
            return Files.readString(Paths.get(uri), StandardCharsets.UTF_8);
        } catch (FileSystemNotFoundException ex) {
            try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                final var path = fs.provider().getPath(uri);
                return Files.readString(path, StandardCharsets.UTF_8);
            }
        }
    }
}
