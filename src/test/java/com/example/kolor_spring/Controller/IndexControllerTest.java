package com.example.kolor_spring.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IndexController.class)
class IndexControllerTest {

    @TestConfiguration
    static class WebClientTestConfig {
        @Bean(name = "webClientBuilder")
        public WebClient.Builder webClientBuilder() {
            WebClient.Builder builderMock = mock(WebClient.Builder.class);
            
            // Use lenient() to allow unused stubs and be specific about types
            lenient().when(builderMock.exchangeStrategies(any(ExchangeStrategies.class))).thenReturn(builderMock);
            lenient().when(builderMock.baseUrl(any(String.class))).thenReturn(builderMock);
            lenient().when(builderMock.clientConnector(any())).thenReturn(builderMock);
            lenient().when(builderMock.build()).thenReturn(mock(WebClient.class));
            
            return builderMock;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testIndexPageLoad() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }
}