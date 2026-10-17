package com.stockmarket.accountservice;

import com.jayway.jsonpath.JsonPath;
import com.stockmarket.accountservice.client.MarketDataClient;
import com.stockmarket.accountservice.client.UserDto;
import com.stockmarket.accountservice.client.UserServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of the account lifecycle against real MySQL (Flyway schema,
 * Hibernate validate) and Kafka in containers. Only market-data-service is mocked.
 *
 * Expected amounts follow the seeded fee schedule and tax rules (V2 migration):
 * brokerage 0.10% (min 1.00), platform 0.50, exchange 0.0035%, withdrawal 2.00,
 * SELL trade tax 0.05%, capital gains 10%, dividend tax 5%.
 */
@SpringBootTest(properties = "account.statements.storage-dir=target/test-uploads")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccountServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MarketDataClient marketDataClient;

    @MockitoBean
    private UserServiceClient userServiceClient;

    private String userId;

    @BeforeEach
    void setUp() {
        userId = "user-" + UUID.randomUUID();
        when(marketDataClient.findCurrentPrice(anyString())).thenReturn(Optional.of(new BigDecimal("110.00")));
        when(userServiceClient.getCurrentUser())
                .thenReturn(new UserDto(userId, "user@example.com", "Asha", "Rao", "ACTIVE", "USER"));
    }

    @Test
    void contextLoads() {
    }

    @Test
    void fullAccountLifecycle() throws Exception {

        // Open
        String accountId = id(call(post("/api/v1/accounts"),
                "{\"accountName\":\"Long-term growth\",\"accountType\":\"INDIVIDUAL\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.balance").value(0)));

        // Deposit 10,000
        call(post("/api/v1/accounts/{id}/deposit", accountId), "{\"amount\":10000.00,\"reference\":\"BANK-1\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balanceAfter").value(10000.00));

        // BUY 10 AAPL @ 100: gross 1000, fees 1.00 + 0.50 + 0.04 = 1.54, no BUY tax
        when(marketDataClient.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("100.00"));
        call(post("/api/v1/trades/buy"), trade(accountId, "aapl", 10))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.grossAmount").value(1000.00))
                .andExpect(jsonPath("$.feeAmount").value(1.54))
                .andExpect(jsonPath("$.taxAmount").value(0))
                .andExpect(jsonPath("$.netAmount").value(1001.54))
                .andExpect(jsonPath("$.balanceAfter").value(8998.46));

        // SELL 4 AAPL @ 120: gross 480, fees 1.00 + 0.50 + 0.02 = 1.52,
        // realized 480 - 400 - 1.52 = 78.48, taxes 0.24 trade + 7.85 CGT = 8.09, net 470.39
        when(marketDataClient.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("120.00"));
        call(post("/api/v1/trades/sell"), trade(accountId, "AAPL", 4))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.feeAmount").value(1.52))
                .andExpect(jsonPath("$.realizedProfit").value(78.48))
                .andExpect(jsonPath("$.taxAmount").value(8.09))
                .andExpect(jsonPath("$.netAmount").value(470.39))
                .andExpect(jsonPath("$.balanceAfter").value(9468.85));

        // Holding: 6 shares at an unchanged average of 100
        call(get("/api/v1/holdings/account/{id}", accountId), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value(6))
                .andExpect(jsonPath("$[0].averagePrice").value(100.0))
                .andExpect(jsonPath("$[0].totalCost").value(600.00))
                .andExpect(jsonPath("$[0].realizedProfit").value(78.48))
                .andExpect(jsonPath("$[0].marketValue").value(660.00))
                .andExpect(jsonPath("$[0].unrealizedProfit").value(60.00));

        // Withdraw 468.85 + 2.00 fee
        call(post("/api/v1/accounts/{id}/withdraw", accountId), "{\"amount\":468.85}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.feeAmount").value(2.00))
                .andExpect(jsonPath("$.balanceAfter").value(8998.00));

        // A USER may not post dividends
        call(post("/api/v1/accounts/{id}/dividends", accountId),
                "{\"symbol\":\"AAPL\",\"dividendPerShare\":0.50,\"paymentDate\":\"" + LocalDate.now() + "\"}")
                .andExpect(status().isForbidden());

        // ADMIN dividend: 6 x 0.50 = 3.00 gross, 0.15 tax, 2.85 net
        mockMvc.perform(post("/api/v1/accounts/{id}/dividends", accountId)
                        .header("X-User-Id", "admin-1")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"AAPL\",\"dividendPerShare\":0.50,\"paymentDate\":\"" + LocalDate.now() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.grossAmount").value(3.00))
                .andExpect(jsonPath("$.taxAmount").value(0.15))
                .andExpect(jsonPath("$.netAmount").value(2.85));

        // Summary totals
        call(get("/api/v1/accounts/{id}/summary", accountId), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashBalance").value(9000.85))
                .andExpect(jsonPath("$.openPositions").value(1))
                .andExpect(jsonPath("$.realizedProfit").value(78.48))
                .andExpect(jsonPath("$.totalFees").value(5.06))
                .andExpect(jsonPath("$.totalTaxes").value(8.24))
                .andExpect(jsonPath("$.totalDividends").value(2.85))
                .andExpect(jsonPath("$.totalDeposits").value(10000.00))
                .andExpect(jsonPath("$.totalWithdrawals").value(468.85))
                .andExpect(jsonPath("$.pricesLive").value(true));

        // Statement for today: opening 0, closing = balance, 10 ledger entries in order
        String today = LocalDate.now().toString();
        call(get("/api/v1/accounts/{id}/statement", accountId).param("from", today).param("to", today), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openingBalance").value(0))
                .andExpect(jsonPath("$.closingBalance").value(9000.85))
                .andExpect(jsonPath("$.ledgerEntries.length()").value(10))
                .andExpect(jsonPath("$.ledgerEntries[0].entryNumber").value(1))
                .andExpect(jsonPath("$.ledgerEntries[9].balanceAfter").value(9000.85))
                .andExpect(jsonPath("$.trades.length()").value(2))
                .andExpect(jsonPath("$.fundTransactions.length()").value(2))
                .andExpect(jsonPath("$.dividends.length()").value(1))
                .andExpect(jsonPath("$.totalFees").value(5.06))
                .andExpect(jsonPath("$.totalTaxes").value(8.24));

        // Audit trail: CREATE, DEPOSIT, 4 per trade (trade, fee and tax calculation, holding update),
        // WITHDRAW, DIVIDEND; entries carry request/response JSON
        call(get("/api/v1/accounts/{id}/audit", accountId), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[?(@.action == 'CREATE')].requestJson").exists());

        // Performance: realized 78.48, unrealized 6 x (110 - 100) = 60, sell fees 1.52 added back;
        // charges 5.06 (all platform fees), taxes 8.24
        call(get("/api/v1/accounts/{id}/performance", accountId), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.realizedProfit").value(78.48))
                .andExpect(jsonPath("$.unrealizedProfit").value(60.00))
                .andExpect(jsonPath("$.grossProfit").value(140.00))
                .andExpect(jsonPath("$.totalCharges").value(5.06))
                .andExpect(jsonPath("$.totalTaxes").value(8.24))
                .andExpect(jsonPath("$.netProfit").value(126.70));

        // Closing with a balance and holdings is refused
        call(delete("/api/v1/accounts/{id}", accountId), null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void businessRulesAndOwnership() throws Exception {

        String accountId = id(call(post("/api/v1/accounts"),
                "{\"accountName\":\"Rules\",\"accountType\":\"JOINT\",\"currency\":\"EUR\"}")
                .andExpect(status().isCreated()));
        call(post("/api/v1/accounts/{id}/deposit", accountId), "{\"amount\":100.00}").andExpect(status().isCreated());
        when(marketDataClient.getCurrentPrice("MSFT")).thenReturn(new BigDecimal("400.00"));

        // Not enough cash: 422 with required/available
        call(post("/api/v1/trades/buy"), trade(accountId, "MSFT", 1))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.available").value(100.00));

        // Selling shares not held: 400
        call(post("/api/v1/trades/sell"), trade(accountId, "MSFT", 1))
                .andExpect(status().isBadRequest());

        // Withdrawal must cover the fee too: 100 + 2 > 100
        call(post("/api/v1/accounts/{id}/withdraw", accountId), "{\"amount\":100.00}")
                .andExpect(status().isUnprocessableEntity());

        // Validation errors per field
        call(post("/api/v1/trades/buy"), "{\"accountId\":\"\",\"symbol\":\"\",\"quantity\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.quantity").exists())
                .andExpect(jsonPath("$.errors.symbol").exists());

        // Another user: 403 everywhere; an ADMIN may read
        mockMvc.perform(get("/api/v1/accounts/{id}", accountId).header("X-User-Id", "intruder"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/accounts/{id}", accountId).header("X-User-Id", "admin-1").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/accounts/user/{userId}", userId).header("X-User-Id", "intruder"))
                .andExpect(status().isForbidden());

        // Missing header: 401; unknown account: 404
        mockMvc.perform(get("/api/v1/accounts/{id}", accountId)).andExpect(status().isUnauthorized());
        call(get("/api/v1/accounts/{id}", "does-not-exist"), null).andExpect(status().isNotFound());

        // Empty out and close: withdraw 98 + 2 fee leaves 0
        call(post("/api/v1/accounts/{id}/withdraw", accountId), "{\"amount\":98.00}")
                .andExpect(jsonPath("$.balanceAfter").value(0));
        call(delete("/api/v1/accounts/{id}", accountId), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
        call(post("/api/v1/accounts/{id}/deposit", accountId), "{\"amount\":1.00}")
                .andExpect(status().isBadRequest());

        call(get("/api/v1/accounts/user/{userId}", userId), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void feeAndTaxPreviews() throws Exception {

        call(get("/api/v1/fees"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        call(post("/api/v1/fees/calculate"), "{\"amount\":50000.00,\"kind\":\"TRADE\"}")
                .andExpect(status().isOk())
                // brokerage 50.00 capped at 25.00, platform 0.50, exchange 1.75
                .andExpect(jsonPath("$.totalFees").value(27.25));

        call(post("/api/v1/taxes/calculate"), "{\"side\":\"SELL\",\"tradeValue\":1000.00,\"realizedProfit\":-50.00}")
                .andExpect(status().isOk())
                // trade tax only: no capital gains tax on a loss
                .andExpect(jsonPath("$.totalTax").value(0.50))
                .andExpect(jsonPath("$.taxes.length()").value(1));
    }

    private ResultActions call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                               String body) throws Exception {
        request.header("X-User-Id", userId);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }

    private static String trade(String accountId, String symbol, int quantity) {
        return "{\"accountId\":\"" + accountId + "\",\"symbol\":\"" + symbol + "\",\"quantity\":" + quantity + "}";
    }

    private static String id(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
