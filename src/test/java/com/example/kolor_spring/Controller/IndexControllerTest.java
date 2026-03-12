package com.example.kolor_spring.Controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Objects;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IndexController.class)
@ContextConfiguration(classes = IndexControllerTest.TestConfig.class)
class IndexControllerTest {

    @Configuration
    static class TestConfig {
        @Bean
        public WebClient.Builder webClientBuilder() {
            WebClient.Builder builder = mock(WebClient.Builder.class);
            WebClient webClient = mock(WebClient.class);
            WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
            WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
            
            // Mock the builder chain
            doReturn(builder).when(builder).exchangeStrategies(mock(ExchangeStrategies.class));
            doReturn(builder).when(builder).baseUrl(anyString());
            doReturn(builder).when(builder).clientConnector(mock(org.springframework.http.client.reactive.ClientHttpConnector.class));
            doReturn(webClient).when(builder).build();
            
            // Mock the request chain
            doReturn(requestBodyUriSpec).when(webClient).post();
            doReturn(requestBodySpec).when(requestBodyUriSpec).uri(anyString());
            doReturn(requestBodySpec).when(requestBodySpec).contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);
            doReturn(requestBodySpec).when(requestBodySpec).body(argThat(Objects::nonNull));
            doReturn(responseSpec).when(requestBodySpec).retrieve();
            
            return builder;
        }
    }

    @Autowired
    private MockMvc mockMvc;


    @Test
    void testIndexPageLoad() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("originalBase64Image", (Object) null))
                .andExpect(model().attribute("correctedImage", (Object) null))
                .andExpect(model().attribute("errorMessage", (Object) null))
                .andExpect(model().attribute("debug", (Object) null));
    }

    @Test
    void testImageCorrectionSuccess() throws Exception {

        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "fake-image-content".getBytes());

        mockMvc.perform(multipart("/correct")
                .file(file)
                .param("lut_name", "C-Log2"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("originalBase64Image"))
                .andExpect(model().attributeExists("correctedImage"))
                .andExpect(model().attributeExists("debug"));
    }

    @Test
    void testImageCorrectionWithEmptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[0]);

        mockMvc.perform(multipart("/correct")
                .file(emptyFile)
                .param("lut_name", "C-Log2"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("originalBase64Image"));
    }

    @Test
    void testImageCorrectionWithInvalidLut() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "fake-content".getBytes());

        mockMvc.perform(multipart("/correct")
                .file(file)
                .param("lut_name", "invalid-lut"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }
}