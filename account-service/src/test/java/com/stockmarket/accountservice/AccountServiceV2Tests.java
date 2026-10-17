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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * v2 flows against real MySQL and Kafka: tracking accounts, manual trades, statement import,
 * charges, taxes, performance, adjustments and user validation.
 */
@SpringBootTest(properties = "account.statements.storage-dir=target/test-uploads")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccountServiceV2Tests {

    private static final String ZERODHA_TRADEBOOK = """
            symbol,isin,trade_date,exchange,segment,series,trade_type,auction,quantity,price,trade_id,order_id,order_execution_time
            INFY,INE009A01021,2026-10-01,NSE,EQ,EQ,buy,false,10,1500,7001,9001,2026-10-01T09:20:00
            INFY,INE009A01021,2026-10-02,NSE,EQ,EQ,buy,false,10,1700,7002,9002,2026-10-02T09:20:00
            INFY,INE009A01021,2026-10-05,NSE,EQ,EQ,sell,false,5,1800,7003,9003,2026-10-05T10:00:00
            INFY,INE009A01021,2026-10-05,NSE,EQ,EQ,sideways,false,5,1800,7004,9004,2026-10-05T10:00:00

            Account Head,Debit
            Brokerage,20
            Securities Transaction Tax,15.5
            Integrated GST,3.6
            Net P&L,980
            """;

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
        when(marketDataClient.findCurrentPrice(anyString())).thenReturn(Optional.empty());
        when(userServiceClient.getCurrentUser())
                .thenReturn(new UserDto(userId, "user@example.com", "Asha", "Rao", "ACTIVE", "USER"));
    }

    @Test
    void zerodhaStatementImportReprocessAndDelete() throws Exception {

        String accountId = openAccount("ZERODHA", "EQUITY");

        // Upload: 3 trades, 1 skipped row, charges + GST tax from the charges table
        String statementId = id(mockMvc.perform(withUser(multipart("/api/v1/accounts/{id}/statements/upload", accountId)
                        .file(csv("tradebook.csv", ZERODHA_TRADEBOOK))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.brokerName").value("ZERODHA"))
                .andExpect(jsonPath("$.fileFormat").value("CSV"))
                .andExpect(jsonPath("$.status").value("PARTIALLY_PROCESSED"))
                .andExpect(jsonPath("$.periodFrom").value("2026-10-01"))
                .andExpect(jsonPath("$.periodTo").value("2026-10-05"))
                .andExpect(jsonPath("$.latestImport.tradesImported").value(3))
                .andExpect(jsonPath("$.latestImport.rowsSkipped").value(1))
                .andExpect(jsonPath("$.latestImport.chargesImported").value(2))
                .andExpect(jsonPath("$.latestImport.taxesImported").value(1))
                .andExpect(jsonPath("$.latestImport.reportedNetPnl").value(980.00))
                .andExpect(jsonPath("$.latestImport.warnings[0]").value(org.hamcrest.Matchers.containsString("sideways"))));

        // Holdings: avg (10 x 1500 + 10 x 1700) / 20 = 1600; 15 left
        call(get("/api/v1/holdings/account/{id}", accountId))
                .andExpect(jsonPath("$[0].symbol").value("INFY"))
                .andExpect(jsonPath("$[0].quantity").value(15))
                .andExpect(jsonPath("$[0].averagePrice").value(1600.0))
                .andExpect(jsonPath("$[0].investedAmount").value(24000.00))
                .andExpect(jsonPath("$[0].realizedProfit").value(1000.00));

        // Imported trades never move cash
        call(get("/api/v1/accounts/{id}", accountId)).andExpect(jsonPath("$.balance").value(0));

        // Same file again: 409 with the existing statement id
        mockMvc.perform(withUser(multipart("/api/v1/accounts/{id}/statements/upload", accountId)
                        .file(csv("copy.csv", ZERODHA_TRADEBOOK))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.existingId").value(statementId));

        // Reprocess replaces the earlier import instead of duplicating it
        call(post("/api/v1/statements/{id}/reprocess", statementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestImport.attemptNumber").value(2))
                .andExpect(jsonPath("$.latestImport.tradesImported").value(3))
                .andExpect(jsonPath("$.latestImport.duplicateTrades").value(0));
        call(get("/api/v1/trades/account/{id}", accountId)).andExpect(jsonPath("$.length()").value(3));
        call(get("/api/v1/statements/{id}/imports", statementId)).andExpect(jsonPath("$.length()").value(2));

        // Download returns the original file
        call(get("/api/v1/statements/{id}/download", statementId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("tradebook.csv")))
                .andExpect(content().string(ZERODHA_TRADEBOOK));

        // Charge and tax summaries
        int year = 2026;
        call(get("/api/v1/charges/account/{id}/summary", accountId).param("year", String.valueOf(year)))
                .andExpect(jsonPath("$.totalCharges").value(35.50))
                .andExpect(jsonPath("$.byType[0].type").value("BROKERAGE"));
        call(get("/api/v1/taxes/account/{id}/summary", accountId).param("year", String.valueOf(year)))
                .andExpect(jsonPath("$.totalTaxes").value(3.60))
                .andExpect(jsonPath("$.byType[0].type").value("GST"));

        // Delete removes everything it imported and rebuilds holdings
        call(delete("/api/v1/statements/{id}", statementId)).andExpect(status().isNoContent());
        call(get("/api/v1/trades/account/{id}", accountId)).andExpect(jsonPath("$.length()").value(0));
        call(get("/api/v1/holdings/account/{id}", accountId)).andExpect(jsonPath("$.length()").value(0));
        call(get("/api/v1/statements/{id}", statementId)).andExpect(status().isNotFound());
    }

    @Test
    void exnessClosedPositionsFeedPerformance() throws Exception {

        String accountId = openAccount("EXNESS", "FOREX");
        String report = """
                ticket,opening_time_utc,closing_time_utc,type,lots,symbol,opening_price,closing_price,commission,swap,profit
                5001,2026-10-01 08:00:00,2026-10-01 12:00:00,buy,0.10,EURUSD,1.0850,1.0900,-0.70,-0.30,50.00
                5002,2026-10-02 09:00:00,2026-10-02 10:30:00,sell,0.20,XAUUSD,2400.00,2390.00,-1.40,0.00,200.00
                """;

        mockMvc.perform(withUser(multipart("/api/v1/accounts/{id}/statements/upload", accountId).file(csv("closed.csv", report))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PROCESSED"))
                .andExpect(jsonPath("$.latestImport.tradesImported").value(2));

        // realized = (50 - 0.70 - 0.30) + (200 - 1.40) = 247.60; gross adds the 2.40 fees back
        call(get("/api/v1/accounts/{id}/performance", accountId))
                .andExpect(jsonPath("$.realizedProfit").value(247.60))
                .andExpect(jsonPath("$.grossProfit").value(250.00))
                .andExpect(jsonPath("$.totalCharges").value(2.40))
                .andExpect(jsonPath("$.netProfit").value(247.60))
                .andExpect(jsonPath("$.brokerName").value("EXNESS"));

        call(get("/api/v1/trades/account/{id}", accountId))
                .andExpect(jsonPath("$[0].tradeKind").value("CLOSED_POSITION"))
                .andExpect(jsonPath("$[0].source").value("IMPORT"))
                .andExpect(jsonPath("$[0].quantity").value(0.2));
    }

    @Test
    void manualTradesOnTrackingAccount() throws Exception {

        String accountId = openAccount("BINANCE", "CRYPTO");

        // fractional BUY
        String tradeId = id(call(post("/api/v1/trades"), manual(accountId, "btcusdt", "BUY", "0.5", "60000", null, "12.50", "7001"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andExpect(jsonPath("$.quantity").value(0.5))
                .andExpect(jsonPath("$.grossAmount").value(30000.00)));

        call(post("/api/v1/trades"), manual(accountId, "BTCUSDT", "SELL", "0.2", null, "70000", "5.00", null))
                .andExpect(status().isCreated());

        // the rebuild computed the SELL's realized profit: (70000 - 60000) x 0.2 - 5 = 1995
        call(get("/api/v1/trades/account/{id}", accountId))
                .andExpect(jsonPath("$[0].side").value("SELL"))
                .andExpect(jsonPath("$[0].realizedProfit").value(1995.00))
                .andExpect(jsonPath("$[0].buyPrice").value(60000.0));
        call(get("/api/v1/trades/{id}", tradeId)).andExpect(status().isOk()).andExpect(jsonPath("$.externalTradeId").value("7001"));
        call(get("/api/v1/holdings/account/{id}", accountId)).andExpect(jsonPath("$[0].quantity").value(0.3));

        // per-trade brokerage became charges
        call(get("/api/v1/charges/account/{id}", accountId))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].chargeType").value("BROKERAGE"));

        // duplicate external id: 409; BUY without buyPrice: 400; platform endpoints refused
        call(post("/api/v1/trades"), manual(accountId, "BTCUSDT", "BUY", "1", "1", null, null, "7001"))
                .andExpect(status().isConflict());
        call(post("/api/v1/trades"), manual(accountId, "BTCUSDT", "BUY", "1", null, "1", null, null))
                .andExpect(status().isBadRequest());
        call(post("/api/v1/trades/buy"), "{\"accountId\":\"" + accountId + "\",\"symbol\":\"AAPL\",\"quantity\":1}")
                .andExpect(status().isBadRequest());

        // statements cannot be uploaded with an unsupported extension
        mockMvc.perform(withUser(multipart("/api/v1/accounts/{id}/statements/upload", accountId)
                        .file(new MockMultipartFile("file", "notes.txt", "text/plain", "x".getBytes()))))
                .andExpect(status().isBadRequest());

        // cash adjustment to reconcile with the broker
        call(post("/api/v1/accounts/{id}/adjustments", accountId), "{\"amount\":250.00,\"description\":\"Opening cash\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("ADJUSTMENT_IN"))
                .andExpect(jsonPath("$.balanceAfter").value(250.00));
        call(post("/api/v1/accounts/{id}/adjustments", accountId), "{\"amount\":-300.00,\"description\":\"Too much\"}")
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void platformAccountsRejectManualTradesAndStatements() throws Exception {

        String accountId = openAccount("PLATFORM", "INDIVIDUAL");

        call(post("/api/v1/trades"), manual(accountId, "AAPL", "BUY", "1", "100", null, null, null))
                .andExpect(status().isBadRequest());
        mockMvc.perform(withUser(multipart("/api/v1/accounts/{id}/statements/upload", accountId)
                        .file(csv("tradebook.csv", ZERODHA_TRADEBOOK))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void accountCreationRequiresAnActiveUser() throws Exception {

        when(userServiceClient.getCurrentUser())
                .thenReturn(new UserDto(userId, "user@example.com", "Asha", "Rao", "SUSPENDED", "USER"));

        call(post("/api/v1/accounts"), "{\"accountName\":\"Blocked\",\"accountType\":\"INDIVIDUAL\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("SUSPENDED")));
    }

    private String openAccount(String broker, String type) throws Exception {
        return id(call(post("/api/v1/accounts"),
                "{\"accountName\":\"" + broker + " " + type + "\",\"accountType\":\"" + type + "\",\"brokerName\":\""
                        + broker + "\",\"remarks\":\"test\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.brokerName").value(broker)));
    }

    private static String manual(String accountId, String symbol, String side, String quantity,
                                 String buyPrice, String sellPrice, String brokerage, String externalId) {
        StringBuilder json = new StringBuilder("{\"accountId\":\"" + accountId + "\",\"symbol\":\"" + symbol
                + "\",\"side\":\"" + side + "\",\"quantity\":" + quantity + ",\"tradeDate\":\"2026-10-0"
                + ("BUY".equals(side) ? "1" : "2") + "T10:00:00\"");
        if (buyPrice != null) json.append(",\"buyPrice\":").append(buyPrice);
        if (sellPrice != null) json.append(",\"sellPrice\":").append(sellPrice);
        if (brokerage != null) json.append(",\"brokerage\":").append(brokerage);
        if (externalId != null) json.append(",\"externalTradeId\":\"").append(externalId).append("\"");
        return json.append("}").toString();
    }

    private static MockMultipartFile csv(String name, String content) {
        return new MockMultipartFile("file", name, "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    private <B extends AbstractMockHttpServletRequestBuilder<B>> B withUser(B request) {
        return request.header("X-User-Id", userId);
    }

    private ResultActions call(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(withUser(request));
    }

    private ResultActions call(MockHttpServletRequestBuilder request, String body) throws Exception {
        return mockMvc.perform(withUser(request).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String id(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
