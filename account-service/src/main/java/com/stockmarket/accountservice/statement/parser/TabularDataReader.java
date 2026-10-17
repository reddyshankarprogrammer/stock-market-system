package com.stockmarket.accountservice.statement.parser;

import com.stockmarket.accountservice.enums.StatementFormat;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads CSV (Apache Commons CSV) and XLS/XLSX (Apache POI) files into {@link TabularData}.
 */
@Component
public class TabularDataReader {

    private static final DateTimeFormatter EXCEL_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * @param maxRows upper bound on rows across all sheets
     * @throws StatementParseException if the file is not a valid file of {@code format} or too large
     */
    public TabularData read(InputStream in, StatementFormat format, int maxRows) {
        try {
            return format == StatementFormat.CSV ? readCsv(in, maxRows) : readWorkbook(in, maxRows);
        } catch (StatementParseException e) {
            throw e;
        } catch (Exception e) {
            throw new StatementParseException("Cannot read " + format + " file: " + e.getMessage(), e);
        }
    }

    private TabularData readCsv(InputStream in, int maxRows) throws IOException {

        String text = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("﻿", "");
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setDelimiter(detectDelimiter(text))
                .setIgnoreSurroundingSpaces(true)
                .setIgnoreEmptyLines(false)
                .get();

        List<List<String>> rows = new ArrayList<>();
        try (CSVParser parser = CSVParser.parse(new StringReader(text), csvFormat)) {
            for (CSVRecord record : parser) {
                checkLimit(rows.size(), maxRows);
                rows.add(record.toList());
            }
        }
        return new TabularData(List.of(new TabularData.Sheet("csv", rows)));
    }

    private TabularData readWorkbook(InputStream in, int maxRows) throws IOException {

        List<TabularData.Sheet> sheets = new ArrayList<>();
        int total = 0;

        try (Workbook workbook = WorkbookFactory.create(in)) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            for (Sheet sheet : workbook) {
                List<List<String>> rows = new ArrayList<>();
                for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                    checkLimit(total++, maxRows);
                    Row row = sheet.getRow(r);
                    List<String> cells = new ArrayList<>();
                    if (row != null) {
                        for (int c = 0; c < Math.max(row.getLastCellNum(), 0); c++) {
                            cells.add(cellText(row.getCell(c), evaluator));
                        }
                    }
                    rows.add(cells);
                }
                sheets.add(new TabularData.Sheet(sheet.getSheetName(), rows));
            }
        }
        return new TabularData(sheets);
    }

    /**
     * Plain text for a cell: numbers without exponent, dates as {@code yyyy-MM-dd HH:mm:ss},
     * formulas as their cached/evaluated result.
     */
    private static String cellText(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        CellType type = cell.getCellType() == CellType.FORMULA
                ? evaluator.evaluateFormulaCell(cell)
                : cell.getCellType();

        return switch (type) {
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().format(EXCEL_DATE)
                    : BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            case STRING -> cell.getStringCellValue();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    /**
     * Comma, semicolon or tab: whichever occurs most in the first non-empty line.
     */
    static char detectDelimiter(String text) {
        String firstLine = text.lines().filter(line -> !line.isBlank()).findFirst().orElse("");
        long commas = firstLine.chars().filter(ch -> ch == ',').count();
        long semicolons = firstLine.chars().filter(ch -> ch == ';').count();
        long tabs = firstLine.chars().filter(ch -> ch == '\t').count();
        if (tabs > commas && tabs > semicolons) {
            return '\t';
        }
        return semicolons > commas ? ';' : ',';
    }

    private static void checkLimit(int rows, int maxRows) {
        if (rows >= maxRows) {
            throw new StatementParseException("File has more than " + maxRows + " rows");
        }
    }
}
