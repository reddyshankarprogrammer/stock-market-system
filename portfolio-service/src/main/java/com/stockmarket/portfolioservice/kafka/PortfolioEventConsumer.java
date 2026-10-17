package com.stockmarket.portfolioservice.kafka;


import com.stockmarket.portfolioservice.entity.Holding;
import com.stockmarket.portfolioservice.repository.HoldingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
public class PortfolioEventConsumer {


    @Autowired
    private HoldingRepository holdingRepository;


    // Consumer group comes from spring.kafka.consumer.group-id.
    @KafkaListener(topics = "${kafka.topics.order-executed}")
    public void consumeOrderExecuted(
            @Payload Map<String, Object> payload) {

        try {

            String userId = (String) payload.get("userId");
            String symbol = (String) payload.get("symbol");
            String type = (String) payload.get("type");

            int quantity = Integer.parseInt(
                    payload.get("quantity").toString());

            BigDecimal price = new BigDecimal(
                    payload.get("price").toString());

            log.info(
                    "Updating portfolio - user: {} {} x {} at {}",
                    userId,
                    symbol,
                    quantity,
                    price);

            if ("BUY".equals(type)) {

                log.info(
                        "Processing BUY order. UserId={}, Symbol={}, Quantity={}, Price={}",
                        userId,
                        symbol,
                        quantity,
                        price);

                addHolding(
                        userId,
                        symbol,
                        quantity,
                        price);

                log.info(
                        "BUY holding updated successfully. UserId={}, Symbol={}",
                        userId,
                        symbol);

            } else if ("SELL".equals(type)) {

                log.info(
                        "Processing SELL order. UserId={}, Symbol={}, Quantity={}",
                        userId,
                        symbol,
                        quantity);

                removeHolding(
                        userId,
                        symbol,
                        quantity);

                log.info(
                        "SELL holding updated successfully. UserId={}, Symbol={}",
                        userId,
                        symbol);
            }

            log.info(
                    "Portfolio update completed successfully. UserId={}, Symbol={}",
                    userId,
                    symbol);

        } catch (Exception e) {

            log.error(
                    "Failed to update portfolio. Payload={}, Error={}",
                    payload,
                    e.getMessage(),
                    e);
        }
    }



    private void addHolding(
            String userId,
            String symbol,
            int quantity,
            BigDecimal price) {

        log.info(
                "Adding holding. UserId={}, Symbol={}, Quantity={}, Price={}",
                userId,
                symbol,
                quantity,
                price);

        Holding holding = holdingRepository
                .findByUserIdAndSymbol(userId, symbol)
                .orElse(null);

        if (holding == null) {

            holding = new Holding();

            holding.setUserId(userId);
            holding.setSymbol(symbol);
            holding.setCompanyName(symbol);

            holding.setQuantity(quantity);
            holding.setAverageBuyPrice(price);
            holding.setTotalInvested(
                    price.multiply(BigDecimal.valueOf(quantity)));

            log.info(
                    "Creating new holding. UserId={}, Symbol={}",
                    userId,
                    symbol);

        } else {

            int oldQuantity = holding.getQuantity();
            int newQuantity = oldQuantity + quantity;

            BigDecimal oldInvestment =
                    holding.getAverageBuyPrice()
                            .multiply(BigDecimal.valueOf(oldQuantity));

            BigDecimal newInvestment =
                    price.multiply(BigDecimal.valueOf(quantity));

            BigDecimal totalInvestment =
                    oldInvestment.add(newInvestment);

            BigDecimal averagePrice =
                    totalInvestment.divide(
                            BigDecimal.valueOf(newQuantity),
                            2,
                            java.math.RoundingMode.HALF_UP);

            holding.setQuantity(newQuantity);
            holding.setAverageBuyPrice(averagePrice);
            holding.setTotalInvested(totalInvestment);

            log.info(
                    "Updated holding. Symbol={}, OldQty={}, NewQty={}, AvgPrice={}",
                    symbol,
                    oldQuantity,
                    newQuantity,
                    averagePrice);
        }

        holdingRepository.save(holding);

        log.info(
                "Holding saved successfully. UserId={}, Symbol={}, Quantity={}",
                userId,
                symbol,
                holding.getQuantity());
    }


    private void removeHolding(
            String userId,
            String symbol,
            int quantity) {

        log.info(
                "Removing holding. UserId={}, Symbol={}, Quantity={}",
                userId,
                symbol,
                quantity);

        Holding holding = holdingRepository
                .findByUserIdAndSymbol(userId, symbol)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Holding not found for symbol: " + symbol));

        int currentQuantity = holding.getQuantity();

        if (currentQuantity < quantity) {

            log.error(
                    "Insufficient holding quantity. Available={}, Requested={}",
                    currentQuantity,
                    quantity);

            throw new RuntimeException(
                    "Insufficient shares available for sell");
        }

        int remainingQuantity = currentQuantity - quantity;

        if (remainingQuantity == 0) {

            holdingRepository.delete(holding);

            log.info(
                    "Holding removed completely. UserId={}, Symbol={}",
                    userId,
                    symbol);

            return;
        }

        holding.setQuantity(remainingQuantity);

        BigDecimal remainingInvestment =
                holding.getAverageBuyPrice()
                        .multiply(BigDecimal.valueOf(remainingQuantity));

        holding.setTotalInvested(remainingInvestment);

        holdingRepository.save(holding);

        log.info(
                "Holding updated after SELL. UserId={}, Symbol={}, RemainingQty={}",
                userId,
                symbol,
                remainingQuantity);
    }



}
