package com.patchlens.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.patchlens.dto.ListModelsRequest;
import com.patchlens.dto.ModelOptionDto;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class ModelDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(ModelDiscoveryService.class);

    private final RestClient restClient;

    public ModelDiscoveryService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public List<ModelOptionDto> listModels(ListModelsRequest request) {
        return listModels(request.baseUrl(), request.apiKey());
    }

    public List<ModelOptionDto> listModels(ModelConfigService.RuntimeModelConfig config) {
        return listModels(config.baseUrl(), config.apiKey());
    }

    private List<ModelOptionDto> listModels(String baseUrl, String apiKey) {
        String modelsUrl = normalizeModelsUrl(baseUrl);
        JsonNode root;
        try {
            root = restClient.get()
                    .uri(modelsUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            log.warn("Failed to fetch model list from {}: status={}, body={}",
                    modelsUrl, ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new IllegalArgumentException("模型列表获取失败：" + ex.getStatusCode().value() + " " + ex.getResponseBodyAsString());
        } catch (RestClientException ex) {
            log.warn("Failed to fetch model list from {}: {}", modelsUrl, ex.getMessage());
            throw new IllegalArgumentException("模型列表获取失败：" + ex.getMessage());
        }
        if (root == null || !root.has("data") || !root.get("data").isArray()) {
            throw new IllegalArgumentException("模型网关返回格式不符合 OpenAI compatible /models 规范");
        }

        List<ModelOptionDto> models = new ArrayList<>();
        for (JsonNode item : root.get("data")) {
            String id = text(item, "id");
            if (!StringUtils.hasText(id)) {
                continue;
            }
            models.add(new ModelOptionDto(id, firstText(item, "owned_by", "owner", "provider")));
        }
        models.sort(Comparator.comparing(ModelOptionDto::id));
        return models;
    }

    private String normalizeModelsUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        normalized = normalizeKnownProviderBaseUrl(normalized);
        if (normalized.endsWith("/chat/completions")) {
            normalized = normalized.substring(0, normalized.length() - "/chat/completions".length());
        }
        if (normalized.endsWith("/responses")) {
            normalized = normalized.substring(0, normalized.length() - "/responses".length());
        }
        if (normalized.endsWith("/models")) {
            return normalized;
        }
        return normalized + "/models";
    }

    private String normalizeKnownProviderBaseUrl(String normalized) {
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

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }
}
