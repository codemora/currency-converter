package com.dexwin.currencyconverter.controller;

import org.assertj.core.matcher.AssertionMatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static java.lang.Double.parseDouble;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class CurrencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void should_convert_EUR_to_USD_with_rate_greater_than_1() throws Exception {
        this.mockMvc.perform(get("/currencies/convert?source=EUR&target=USD&amount=1"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        new AssertionMatcher<>() {
                            @Override
                            public void assertion(String value) throws AssertionError {
                                assertThat(parseDouble(value)).isGreaterThan(1.0);
                            }
                        })
                );
    }

    @Test
    public void should_convert_USD_to_EUR_with_rate_less_than_1() throws Exception {
        this.mockMvc.perform(get("/currencies/convert?source=USD&target=EUR&amount=1"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        new AssertionMatcher<>() {
                            @Override
                            public void assertion(String value) throws AssertionError {
                                assertThat(parseDouble(value)).isLessThan(1.0);
                            }
                        })
                );
    }

    @Test
    public void should_return_bad_request_for_invalid_source_currency() throws Exception {
        this.mockMvc.perform(get("/currencies/convert?source=XYZ&target=USD&amount=1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid source currency"));
    }

    @Test
    public void should_return_bad_request_for_invalid_target_currency() throws Exception {
        this.mockMvc.perform(get("/currencies/convert?source=EUR&target=XYZ&amount=1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid target currency"));
    }

    @Test
    public void should_return_bad_request_for_zero_amount() throws Exception {
        this.mockMvc.perform(get("/currencies/convert?source=EUR&target=USD&amount=0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Amount must be greater than zero"));
    }

    @Test
    public void should_return_bad_request_for_negative_amount() throws Exception {
        this.mockMvc.perform(get("/currencies/convert?source=EUR&target=USD&amount=-5"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Amount must be greater than zero"));
    }

    @Test
    public void should_return_same_amount_when_source_and_target_are_same() throws Exception {
        this.mockMvc.perform(get("/currencies/convert?source=USD&target=USD&amount=100"))
                .andExpect(status().isOk())
                .andExpect(content().string("100.0"));
    }

    @Test
    public void should_return_bad_request_when_source_is_blank() throws Exception {
        this.mockMvc.perform(get("/currencies/convert?source=&target=USD&amount=100"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Source currency must be provided"));
    }

    @Test
    public void should_return_bad_request_when_target_is_blank() throws Exception {
        this.mockMvc.perform(get("/currencies/convert?source=USD&target=&amount=100"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Target currency must be provided"));
    }

    @Test
    public void should_return_bad_request_when_amount_is_blank() throws Exception {
        this.mockMvc.perform(get("/currencies/convert?source=USD&target=&amount=100"))
                .andExpect(status().isBadRequest());
    }
}