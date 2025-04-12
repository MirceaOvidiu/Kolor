package com.example.kolor_spring.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

@Controller
public class indexController {

    private final WebClient webClient;

    public indexController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("/http://localhost:8080")
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs()
                                .maxInMemorySize(16 * 1024 * 1024)) // 16MB
                        .build())
                .build();
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/correct")
    public String correctImage(@RequestParam("image") MultipartFile image,
                               @RequestParam("lut_name") String lut,
                               Model model) {

        Mono<String> response = webClient.post()
                .uri("http://127.0.0.1:5000" + "/correctAPI")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file", image.getResource())
                        .with("lut_name", lut))
                .retrieve()
                .bodyToMono(String.class);

        String correctedImage64 = response.block();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(correctedImage64);
            String correctedImageBase64 = rootNode.path("image").asText();
            model.addAttribute("correctedImage", correctedImageBase64); // Directly pass the Base64 string
        } catch (JsonProcessingException e) {
            model.addAttribute("errorMessage", "Failed to process the corrected image.");
            return "index";
        }
        return "index";
    }
}
