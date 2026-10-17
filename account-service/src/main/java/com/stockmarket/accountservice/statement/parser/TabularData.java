package com.stockmarket.accountservice.statement.parser;

import java.util.List;

/**
 * A statement file read into plain text cells: one or more sheets (CSV has one),
 * each a list of rows, each a list of cell strings.
 *
 * Numbers are plain decimal strings and dates from Excel date cells are ISO
 * {@code yyyy-MM-dd HH:mm:ss}, so parsers do not depend on the file format.
 */
public record TabularData(List<Sheet> sheets) {

    public record Sheet(String name, List<List<String>> rows) {
    }

    public int totalRows() {
        return sheets.stream().mapToInt(sheet -> sheet.rows().size()).sum();
    }
}
