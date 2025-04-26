package com.example.kolor_spring.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.MultipartBodyBuilder;
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

import java.io.IOException;
import java.util.Base64;

@Controller
public class IndexController {

    private final WebClient webClient;
    private static final Logger myLogger = LoggerFactory.getLogger(IndexController.class);

    public IndexController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs()
                                .maxInMemorySize(16 * 1024 * 1024)) // 16MB
                        .build())
                .build();
    }

    @GetMapping("/")
    public String index(Model model) {
        // Clear any previously displayed images or error messages
        model.addAttribute("originalBase64Image", null);
        model.addAttribute("correctedImage", null);
        model.addAttribute("errorMessage", null);
        model.addAttribute("debug", null);
        return "index";
    }

    @PostMapping("/correct")
    public String correctImage(@RequestParam("image") MultipartFile image,
                               @RequestParam("lut_name") String lut,
                               Model model) {
        try {
            String originalBase64Image = "data:image/png;base64," + Base64.getEncoder().encodeToString(image.getBytes());
            model.addAttribute("originalBase64Image", originalBase64Image);

            // Create multipart body builder
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();

            // Add the file data
            bodyBuilder.part("file", new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename();
                }
            });

            // Add the LUT name
            bodyBuilder.part("lut_name", lut);

            // Debug info
            System.out.println("Sending request to Flask API with LUT: " + lut);
            System.out.println("Original image size: " + image.getSize() + " bytes");

            final String dockerAPIUrl = "http://python-service:5000";
            final String k8sAPIUrl = "http://python-service-service:5000";

            // Make request to Flask API
            byte[] imageBytes = webClient.post()
                    .uri(k8sAPIUrl + "/correctAPI")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            // Debug response
            System.out.println("Response received from API. Bytes length: " +
                    (imageBytes != null ? imageBytes.length : "null"));

            if (imageBytes != null && imageBytes.length > 0) {
                // Convert image bytes to base64
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                model.addAttribute("correctedImage", "data:image/png;base64," + base64Image);
                model.addAttribute("debug", "Image processed successfully. Size: " + imageBytes.length + " bytes");
            } else {
                model.addAttribute("errorMessage", "Failed to retrieve corrected image from API or empty response.");
            }
        } catch (IOException e) {
            myLogger.error("Failed to process the image file: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Failed to process the image file: " + e.getMessage());
        } catch (Exception e) {
            myLogger.error("An unexpected error occurred: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "An error occurred: " + e.getMessage());
        }

        return "index";
    }
}
