package com.example.kolor_spring.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Base64;

@Controller
public class indexController {

    private final WebClient webClient;

    public indexController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8000").build();
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/correct")
    public String correctImage(@RequestParam("image") MultipartFile image,
                               @RequestParam("lut") String lut,
                               Model model) throws IOException {
        byte[] imageBytes = image.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        Mono<String> response = webClient.post()
                .uri("/color_correct/")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("file", image.getResource())
                        .with("lut_name", lut))
                .retrieve()
                .bodyToMono(String.class);

        String correctedImage = response.block();
        model.addAttribute("correctedImage", correctedImage);
        return "index";
    }
}
