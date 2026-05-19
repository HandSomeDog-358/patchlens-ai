package com.patchlens.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patchlens.domain.FindingSeverity;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "patchlens.ai-reviewer", havingValue = "spring-ai")
public class SpringAiReviewClient implements AiReviewClient {

    private static final int MAX_DIFF_CHARS = 30_000;

    private final ModelConfigService modelConfigService;
    private final OpenAiCompatibleModelFactory modelFactory;
    private final ObjectMapper objectMapper;

    public SpringAiReviewClient(
            ModelConfigService modelConfigService,
            OpenAiCompatibleModelFactory modelFactory,
            ObjectMapper objectMapper
    ) {
        this.modelConfigService = modelConfigService;
        this.modelFactory = modelFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public ReviewResult review(ReviewInput input) {
        ChatClient chatClient = ChatClient.builder(buildChatModel()).build();
        String response = chatClient.prompt()
                .system(systemPrompt())
                .user(userPrompt(input))
                .call()
                .content();
        return parseResponse(response, input);
    }

    private OpenAiChatModel buildChatModel() {
        ModelConfigService.RuntimeModelConfig config = modelConfigService.getEnabledRuntimeConfig();
        return modelFactory.create(config.baseUrl(), config.apiKey(), config.modelName());
    }

    private String systemPrompt() {
        return """
                你是 PatchLens AI，一个谨慎、低噪音的资深代码审查 Agent。
                你的任务是审查 Pull Request 变更，找出有证据支撑的业务风险、逻辑风险、安全风险和测试缺口。
                必须服从用户提供的质量策略。低于最低置信度的发现不要输出；忽略路径不要输出发现；如果关闭修复建议，patch 返回 null。
                只输出 JSON，不要输出 Markdown，不要包裹 ```json。
                JSON schema:
                {
                  "summary": "简洁中文摘要",
                  "findings": [
                    {
                      "severity": "CRITICAL|HIGH|MEDIUM|LOW",
                      "confidence": 0.0,
                      "filePath": "文件路径",
                      "lineNumber": 1,
                      "title": "风险标题",
                      "description": "问题说明",
                      "evidence": "基于 diff 的证据",
                      "suggestion": "具体修改建议",
                      "patch": null
                    }
                  ]
                }
                如果没有高置信度问题，findings 返回空数组。
                """;
    }

    private String userPrompt(ReviewInput input) {
        return """
                仓库：%s
                审查目标类型：%s
                审查目标：%s
                PR 编号：%d
                Commit：%s
                标题：%s
                描述：%s
                变更文件：%s
                质量策略：
                - 反馈语言：%s
                - 最低置信度：%.2f
                - 最多发现数量：%d
                - 生成摘要：%s
                - 生成行级发现：%s
                - 生成修复建议：%s
                - 忽略路径：
                %s
                - 重点路径：
                %s

                Diff:
                %s
                """.formatted(
                input.repositoryName(),
                input.targetType(),
                input.targetRef(),
                input.prNumber(),
                input.commitSha(),
                blankToFallback(input.title(), "无"),
                blankToFallback(input.description(), "无"),
                input.changedFiles().isEmpty() ? "无" : String.join(", ", input.changedFiles()),
                blankToFallback(input.language(), "zh-CN"),
                input.minConfidence(),
                input.maxInlineComments(),
                input.enableSummary() ? "是" : "否",
                input.enableInlineComments() ? "是" : "否",
                input.enableSuggestedPatch() ? "是" : "否",
                blankToFallback(input.ignoredPaths(), "无"),
                blankToFallback(input.focusPaths(), "无"),
                truncate(blankToFallback(input.diff(), "无 diff 内容"))
        );
    }

    private ReviewResult parseResponse(String response, ReviewInput input) {
        try {
            JsonNode root = objectMapper.readTree(stripJsonFence(response));
            String summary = root.path("summary").asText(defaultSummary(input));
            List<FindingCandidate> findings = new ArrayList<>();
            JsonNode findingsNode = root.path("findings");
            if (findingsNode.isArray()) {
                for (JsonNode node : findingsNode) {
                    findings.add(new FindingCandidate(
                            parseSeverity(node.path("severity").asText("LOW")),
                            clamp(node.path("confidence").asDouble(0.0)),
                            node.path("filePath").asText("unknown"),
                            Math.max(1, node.path("lineNumber").asInt(1)),
                            node.path("title").asText("AI 审查发现"),
                            node.path("description").asText(""),
                            node.path("evidence").asText(""),
                            node.path("suggestion").asText(""),
                            nullIfBlank(node.path("patch").asText(null))
                    ));
                }
            }
            return new ReviewResult(summary, findings);
        } catch (RuntimeException ex) {
            return new ReviewResult(defaultSummary(input) + " 模型输出解析失败，已保留为无风险结果。", List.of());
        } catch (Exception ex) {
            return new ReviewResult(defaultSummary(input) + " 模型输出解析失败，已保留为无风险结果。", List.of());
        }
    }

    private String stripJsonFence(String response) {
        if (response == null) {
            return "{}";
        }
        String trimmed = response.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed;
    }

    private FindingSeverity parseSeverity(String value) {
        try {
            return FindingSeverity.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return FindingSeverity.LOW;
        }
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private String truncate(String value) {
        if (value.length() <= MAX_DIFF_CHARS) {
            return value;
        }
        return value.substring(0, MAX_DIFF_CHARS) + "\n[diff truncated]";
    }

    private String blankToFallback(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String nullIfBlank(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private String defaultSummary(ReviewInput input) {
        return "PatchLens AI 已使用大模型审查 " + input.targetType() + " " + input.targetRef() + "。";
    }
}
