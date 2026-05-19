package com.patchlens.service;

import com.patchlens.dto.ModelConfigTestResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ModelConfigTestService {

    private final ModelConfigService modelConfigService;
    private final OpenAiCompatibleModelFactory modelFactory;

    public ModelConfigTestService(
            ModelConfigService modelConfigService,
            OpenAiCompatibleModelFactory modelFactory
    ) {
        this.modelConfigService = modelConfigService;
        this.modelFactory = modelFactory;
    }

    public ModelConfigTestResult test(Long id) {
        long start = System.nanoTime();
        try {
            ModelConfigService.RuntimeModelConfig config = modelConfigService.getRuntimeConfig(id);
            ChatClient chatClient = ChatClient.builder(
                    modelFactory.create(config.baseUrl(), config.apiKey(), config.modelName())
            ).build();
            String content = chatClient.prompt()
                    .system("你是模型连通性测试助手。")
                    .user("请只回复 OK。")
                    .call()
                    .content();
            long latencyMs = (System.nanoTime() - start) / 1_000_000;
            if (!StringUtils.hasText(content)) {
                return new ModelConfigTestResult(false, "模型返回为空", latencyMs);
            }
            return new ModelConfigTestResult(true, "模型连接正常，返回：" + content.strip(), latencyMs);
        } catch (RuntimeException ex) {
            long latencyMs = (System.nanoTime() - start) / 1_000_000;
            return new ModelConfigTestResult(false, ex.getMessage(), latencyMs);
        }
    }
}
