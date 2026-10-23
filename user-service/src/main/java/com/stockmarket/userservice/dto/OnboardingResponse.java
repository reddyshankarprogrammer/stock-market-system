package com.stockmarket.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingResponse {

    private String tradingExperience;
    private List<String> brokerPreferences;
    private String plan;
    private boolean completed;
    private LocalDateTime completedAt;
}
