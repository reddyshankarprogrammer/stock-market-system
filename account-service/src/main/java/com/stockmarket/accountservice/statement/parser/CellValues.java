package com.stockmarket.accountservice.statement.parser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lenient parsing of statement cell text.
 */
public final class CellValues {

    private static final Pattern NUMBER = Pattern.compile("[-+]?\\d[\\d,]*(\\.\\d+)?|[-+]?\\.\\d+");

    /**
     * Always tried after the broker's own patterns; the first matches Excel date cells.
     */
    private static final List<String> DEFAULT_DATE_FORMATS = List.of(
            "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd");

    private CellValues() {
    }

    /**
     * Lower-case, trimmed, single-spaced, without a byte-order mark or surrounding quotes.
     */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String value = text.replace("﻿", "").trim();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    /**
     * The first number in the text, so "₹1,234.50", "0.5BTC", "-0.70 USD" and "(12.30)" all
     * parse; parentheses mean negative. Null for blank text.
     *
     * @throws IllegalArgumentException if the text contains no number
     */
    public static BigDecimal number(String text) {
        if (isBlank(text)) {
            return null;
        }
        String value = text.trim();
        Matcher matcher = NUMBER.matcher(value);
        if (!matcher.find()) {
            throw new IllegalArgumentException("not a number: '" + value + "'");
        }
        BigDecimal number = new BigDecimal(matcher.group().replace(",", ""));
        boolean parenthesized = value.startsWith("(") && value.endsWith(")");
        return parenthesized ? number.abs().negate() : number;
    }

    /**
     * Parses a date or date-time with the given patterns, then the defaults. A date alone
     * becomes midnight.
     *
     * @throws IllegalArgumentException if no pattern matches
     */
    public static LocalDateTime dateTime(String text, List<String> patterns) {
        if (isBlank(text)) {
            throw new IllegalArgumentException("date is empty");
        }
        String value = text.trim();
        List<String> all = new ArrayList<>(patterns);
        all.addAll(DEFAULT_DATE_FORMATS);

        for (String pattern : all) {
            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern(pattern)
                    .toFormatter(Locale.ENGLISH);
            try {
                TemporalAccessor parsed = formatter.parseBest(value, LocalDateTime::from, LocalDate::from);
                return parsed instanceof LocalDateTime dateTime ? dateTime : ((LocalDate) parsed).atStartOfDay();
            } catch (DateTimeParseException e) {
                // try the next pattern
            }
        }
        throw new IllegalArgumentException("unrecognized date: '" + value + "'");
    }
}
