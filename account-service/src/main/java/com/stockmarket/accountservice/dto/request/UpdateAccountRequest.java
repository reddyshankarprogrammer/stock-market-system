package com.stockmarket.accountservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Changes the editable details of an account.
 */
@Schema(description = "Changes the editable details of an account")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccountRequest {

    /**
     * New display name.
     */
    @NotBlank(message = "Account name is required")
    @Size(max = 100, message = "Account name must be at most 100 characters")
    private String accountName;

    /**
     * New remarks; omit to keep the current ones.
     */
    @Size(max = 500, message = "Remarks must be at most 500 characters")
    private String remarks;
}
