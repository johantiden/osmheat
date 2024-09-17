package com.github.johantiden.osmheat.cache;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

public class FileCache<V> {
    private static final Logger logger = LoggerFactory.getLogger(FileCache.class);
    private final Path basePath;
    private final CheckedFunction<V, byte[]> serializer;
    private final CheckedFunction<byte[], V> deserializer;
    private final String fileExtension;

    public FileCache(Path basePath, CheckedFunction<V, byte[]> serializer, CheckedFunction<byte[], V> deserializer, String fileExtension) {
        this.basePath = basePath;
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.fileExtension = fileExtension;
    }

    public Path getBasePath() {
        return basePath;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public V get(String key, Callable<V> callable) throws Exception {
        String intern = key.intern();
        synchronized (intern) {
            Path path = getFullPath(key);
            if (Files.exists(path)) {
                logger.info("{} exists, returning", path);
                byte[] bytes = Files.readAllBytes(path);
                return deserializer.apply(bytes);
            } else {
                logger.info("{} miss, calculating...", path);
                V value = callable.call();
                byte[] bytes = serializer.apply(value);
                Files.createDirectories(path.getParent());
                Files.createFile(path);
                Files.write(path, bytes);
                return value;
            }
        }
    }

    @Nonnull
    public Path getFullPath(String key) {
        return basePath.resolve(key + fileExtension).normalize();
    }
}
