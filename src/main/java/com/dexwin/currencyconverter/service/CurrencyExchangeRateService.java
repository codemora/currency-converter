package com.dexwin.currencyconverter.service;

import com.dexwin.currencyconverter.configs.ExchangeRateConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.Optional;

@Service
public class CurrencyExchangeRateService implements CurrencyService {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyExchangeRateService.class);

    private final RestTemplate restTemplate;
    private final ExchangeRateConfiguration exchangeRateConfig;
    private final ObjectMapper objectMapper;

    public CurrencyExchangeRateService(RestTemplate restTemplate, ExchangeRateConfiguration exchangeRateConfig, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.exchangeRateConfig = exchangeRateConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    public double convert(String source, String target, double amount) {
        if (source.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source currency must be provided");
        }
        if (target.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target currency must be provided");
        }
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        }
        if (Objects.equals(source, target)) {
            return amount;
        }
        return getExchangeRate(source, target)
                .map(rate -> rate * amount)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exchange rate not available"));
    }

    private Optional<Double> getExchangeRate(String source, String target) {
        String requestUrl = String.format("%s?access_key=%s&source=%s&currencies=%s",
                exchangeRateConfig.getBaseUrl(),
                exchangeRateConfig.getAccessKey(),
                source,
                target);

        logger.info("Sending API request to: {}", requestUrl);

        ResponseEntity<String> response = fetchApiResponse(requestUrl);
        JsonNode root = parseJson(response.getBody());

        if (!root.path("success").asBoolean()) {
            handleApiError(root);
        }

        String quoteKey = source + target;
        double exchangeRate = root.path("quotes").path(quoteKey.toUpperCase()).asDouble(Double.NaN);

        if (Double.isNaN(exchangeRate)) {
            logger.warn("Exchange rate not found for {} -> {}", source, target);
            return Optional.empty();
        }

        return Optional.of(exchangeRate);
    }

    private ResponseEntity<String> fetchApiResponse(String url) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            logger.info("Received API response: Status Code = {}", response.getStatusCode());
            return response;
        } catch (RestClientException ex) {
            logger.error("Exception while fetching exchange rate: ", ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error calling exchange rate API", ex);
        }
    }

    private JsonNode parseJson(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing JSON response", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error parsing API response");
        }
    }

    private void handleApiError(JsonNode root) {
        JsonNode errorNode = root.path("error");
        int errorCode = errorNode.path("code").asInt();
        String errorMessage = errorNode.path("info").asText();

        logger.error("API responded with error: {} {}", errorCode, errorMessage);

        switch (errorCode) {
            case 101:
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing access key");
            case 201:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid source currency");
            case 202:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid target currency");
            default:
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Unable to process request");
        }

    }
}

