package com.stockmarket.userservice.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Every field is optional and only the fields that are sent change, so one switch can be saved on its own. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferencesRequest {

    @Pattern(regexp = "^(SYSTEM|DARK|LIGHT)$", message = "Theme should be SYSTEM, DARK or LIGHT")
    private String theme;

    @Pattern(regexp = "^en$", message = "Language should be en")
    private String language;

    private Boolean notifyInApp;
    private Boolean notifyPush;
    private Boolean notifyEmail;

    @Pattern(regexp = "^(CSV|EXCEL|PDF|JSON)$", message = "Export format should be CSV, EXCEL, PDF or JSON")
    private String exportFormat;
}
