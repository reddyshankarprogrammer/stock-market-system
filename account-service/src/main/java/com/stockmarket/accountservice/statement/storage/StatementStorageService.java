package com.stockmarket.accountservice.statement.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Stores statement files on the local file system.
 *
 * Stored names are generated ({@code <accountId>/<statementId>.<ext>}), never taken from the
 * uploaded file name, and every resolved path is checked to stay inside the base directory.
 * To move to object storage (S3, Azure Blob), replace this class; callers only use
 * relative paths.
 */
@Slf4j
@Component
public class StatementStorageService {

    private final Path baseDir;

    public StatementStorageService(StatementStorageProperties properties) {
        this.baseDir = Path.of(properties.getStorageDir()).toAbsolutePath().normalize();
    }

    /**
     * Writes the file and returns its path relative to the base directory.
     */
    public String store(String accountId, String statementId, String extension, InputStream content) {

        String relative = accountId + "/" + statementId + "." + extension;
        Path target = resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store statement " + statementId, e);
        }
        log.info("Stored statement {} at {}", statementId, target);
        return relative;
    }

    public InputStream open(String relativePath) {
        try {
            return Files.newInputStream(resolve(relativePath));
        } catch (IOException e) {
            throw new UncheckedIOException("Statement file missing: " + relativePath, e);
        }
    }

    public Resource load(String relativePath) {
        Path path = resolve(relativePath);
        if (!Files.isReadable(path)) {
            throw new UncheckedIOException(new IOException("Statement file missing: " + relativePath));
        }
        return new PathResource(path);
    }

    /**
     * Deletes the file; a missing file is logged, not an error.
     */
    public void delete(String relativePath) {
        try {
            if (!Files.deleteIfExists(resolve(relativePath))) {
                log.warn("Statement file already gone: {}", relativePath);
            }
        } catch (IOException e) {
            log.error("Could not delete statement file {}", relativePath, e);
        }
    }

    private Path resolve(String relativePath) {
        Path path = baseDir.resolve(relativePath).normalize();
        if (!path.startsWith(baseDir)) {
            throw new IllegalArgumentException("Path escapes the statement directory: " + relativePath);
        }
        return path;
    }
}
