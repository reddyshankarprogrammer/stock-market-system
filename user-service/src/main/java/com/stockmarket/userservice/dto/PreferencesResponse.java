package com.stockmarket.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferencesResponse {

    private String theme;
    private String language;
    private boolean notifyInApp;
    private boolean notifyPush;
    private boolean notifyEmail;
    private String exportFormat;
}
