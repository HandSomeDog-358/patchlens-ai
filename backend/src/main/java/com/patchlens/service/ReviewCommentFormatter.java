package com.patchlens.service;

import com.patchlens.domain.ReviewConclusion;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReviewCommentFormatter {

    public String format(
            String repositoryName,
            String targetRef,
            String commitSha,
            ReviewConclusion conclusion,
            String summary,
            List<AiReviewClient.FindingCandidate> findings
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("## PatchLens AI 审查结果：").append(conclusion.name()).append("\n\n");
        builder.append("- 仓库：").append(repositoryName).append("\n");
        builder.append("- 目标：").append(targetRef).append("\n");
        builder.append("- Commit：").append(commitSha).append("\n");
        builder.append("- 风险发现：").append(findings.size()).append(" 条\n\n");
        if (StringUtils.hasText(summary)) {
            builder.append("### 摘要\n").append(summary).append("\n\n");
        }
        if (findings.isEmpty()) {
            builder.append("### 发现\n未发现达到当前质量策略阈值的风险。\n");
            return builder.toString();
        }
        builder.append("### 发现\n");
        int index = 1;
        for (AiReviewClient.FindingCandidate finding : findings) {
            builder.append(index++).append(". **[").append(finding.severity()).append("] ")
                    .append(finding.title()).append("**\n");
            builder.append("   - 位置：").append(finding.filePath()).append(":").append(finding.lineNumber()).append("\n");
            builder.append("   - 置信度：").append(Math.round(finding.confidence() * 100)).append("%\n");
            if (StringUtils.hasText(finding.description())) {
                builder.append("   - 问题：").append(finding.description()).append("\n");
            }
            if (StringUtils.hasText(finding.suggestion())) {
                builder.append("   - 建议：").append(finding.suggestion()).append("\n");
            }
        }
        return builder.toString();
    }
}
