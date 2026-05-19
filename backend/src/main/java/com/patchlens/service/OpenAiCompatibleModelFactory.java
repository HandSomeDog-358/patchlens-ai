package com.patchlens.service;

import io.micrometer.observation.ObservationRegistry;
import java.net.URI;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class OpenAiCompatibleModelFactory {

    private final RestClient.Builder restClientBuilder;

    public OpenAiCompatibleModelFactory(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    public OpenAiChatModel create(String baseUrl, String apiKey, String modelName) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(normalizeBaseUrl(baseUrl))
                .completionsPath("/chat/completions")
                .embeddingsPath("/embeddings")
                .apiKey(apiKey)
                .restClientBuilder(restClientBuilder)
                .webClientBuilder(WebClient.builder())
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(modelName)
                .temperature(0.1)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .toolCallingManager(ToolCallingManager.builder().build())
                .retryTemplate(RetryTemplate.builder().maxAttempts(2).fixedBackoff(1000).build())
                .observationRegistry(ObservationRegistry.NOOP)
                .build();
    }

    public String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return baseUrl;
        }
        String normalized = baseUrl.strip();
        String chatCompletionsPath = "/chat/completions";
        if (normalized.endsWith(chatCompletionsPath)) {
            normalized = normalized.substring(0, normalized.length() - chatCompletionsPath.length());
        }
        try {
            URI uri = URI.create(normalized);
            if ("dashscope.aliyuncs.com".equalsIgnoreCase(uri.getHost())) {
                String path = uri.getPath();
                if (!StringUtils.hasText(path) || "/".equals(path)) {
                    return normalized + "/compatible-mode/v1";
                }
            }
        } catch (IllegalArgumentException ignored) {
            return normalized;
        }
        return normalized;
    }
}
