package com.patchlens.service;

import com.patchlens.domain.FindingSeverity;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(AiReviewClient.class)
public class MockAiReviewClient implements AiReviewClient {

    @Override
    public ReviewResult review(ReviewInput input) {
        if (!input.enableInlineComments()) {
            return new ReviewResult("PatchLens AI 已按质量策略跳过行级发现，仅生成审查摘要。", List.of());
        }
        String title = input.title() == null || input.title().isBlank() ? "未获取到 PR 标题" : input.title();
        String fileSummary = input.changedFiles().isEmpty()
                ? "未获取到变更文件"
                : "变更文件：" + String.join(", ", input.changedFiles());
        String summary = "PatchLens AI 已创建审查任务。当前为本地 mock 审查器，后续会替换为 Spring AI 模型调用。"
                + " 目标：" + input.targetType() + " " + input.targetRef() + "，commit " + input.commitSha() + "。"
                + " 标题：" + title + "。"
                + " " + fileSummary + "。";

        FindingCandidate finding = new FindingCandidate(
                FindingSeverity.MEDIUM,
                0.86,
                "src/main/java/example/ReviewTarget.java",
                42,
                "需要接入真实 diff 后进行风险定位",
                "当前审查任务尚未接入代码托管平台的 diff 拉取，因此无法定位真实代码风险。",
                "Mock reviewer 只用于验证任务、入库和前端展示链路。",
                "下一步接入 GitHub / Gitee Pull Request API，并将 diff 交给 Spring AI 生成结构化 findings。",
                null
        );

        return new ReviewResult(summary, List.of(finding));
    }
}
