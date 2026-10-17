package com.stockmarket.accountservice.statement.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Statement file storage ({@code account.statements.*}).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "account.statements")
public class StatementStorageProperties {

    /**
     * Base directory for uploaded statements, relative to the working directory or absolute.
     * Files are stored as {@code <accountId>/<statementId>.<ext>}.
     */
    private String storageDir = "uploads/statements";

    /**
     * Largest accepted file. Keep spring.servlet.multipart.max-file-size in line with it.
     */
    private DataSize maxFileSize = DataSize.ofMegabytes(20);
}
