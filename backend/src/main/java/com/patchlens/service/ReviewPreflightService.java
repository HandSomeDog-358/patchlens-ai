package com.patchlens.service;

import com.patchlens.domain.RepositoryConnection;
import com.patchlens.domain.RepositoryProvider;
import com.patchlens.dto.CreateCommitReviewRequest;
import com.patchlens.dto.ReviewPreflightCheckDto;
import com.patchlens.dto.ReviewPreflightResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ReviewPreflightService {

    private final RepositoryService repositoryService;
    private final PlatformConfigService platformConfigService;
    private final ModelConfigService modelConfigService;
    private final PullRequestContextLoader pullRequestContextLoader;

    public ReviewPreflightService(
            RepositoryService repositoryService,
            PlatformConfigService platformConfigService,
            ModelConfigService modelConfigService,
            PullRequestContextLoader pullRequestContextLoader
    ) {
        this.repositoryService = repositoryService;
        this.platformConfigService = platformConfigService;
        this.modelConfigService = modelConfigService;
        this.pullRequestContextLoader = pullRequestContextLoader;
    }

    @Transactional(readOnly = true)
    public ReviewPreflightResponse preflightCommitReview(Long repositoryId, CreateCommitReviewRequest request) {
        List<ReviewPreflightCheckDto> checks = new ArrayList<>();
        RepositoryConnection repository = repositoryService.getRepository(repositoryId);

        if (!StringUtils.hasText(request.commitSha())) {
            checks.add(fail("Commit", "请先选择或输入 commit 编码"));
        } else {
            checks.add(ok("Commit", "commit 编码格式已填写"));
        }

        if (repository.getProvider() != RepositoryProvider.GITEE && repository.getProvider() != RepositoryProvider.GITEA) {
            checks.add(fail("平台配置", "当前只支持 Gitee / Gitea 拉取 commit diff"));
        } else {
            boolean hasApiAccess = platformConfigService.resolve(repository.getProvider())
                    .map(PlatformConfigService.ResolvedPlatformConfig::hasApiAccess)
                    .orElse(false);
            if (hasApiAccess) {
                checks.add(ok("平台配置", repository.getProvider() + " API 配置可用"));
            } else {
                checks.add(fail("平台配置", repository.getProvider() + " 缺少可用的 API Base URL 或访问令牌"));
            }
        }

        try {
            ModelConfigService.RuntimeModelConfig model = modelConfigService.getEnabledRuntimeConfig();
            checks.add(ok("模型配置", "当前模型：" + model.provider() + " / " + model.modelName()));
        } catch (RuntimeException ex) {
            checks.add(fail("模型配置", ex.getMessage()));
        }

        if (StringUtils.hasText(request.commitSha())) {
            try {
                PullRequestContext context = pullRequestContextLoader.loadCommit(repository, request.commitSha());
                if (context.changedFiles().isEmpty()) {
                    checks.add(warn("Commit 内容", "已找到 commit，但平台没有返回变更文件列表"));
                } else {
                    checks.add(ok("Commit 内容", "变更文件 " + context.changedFiles().size() + " 个"));
                }
                String usefulDiff = context.diff() == null
                        ? ""
                        : context.diff()
                                .replace("[No patch returned by Gitee API]", "")
                                .replaceAll("(?m)^### .*$", "")
                                .trim();
                if (StringUtils.hasText(usefulDiff)) {
                    if (context.diff().contains("[No patch returned")) {
                        checks.add(warn("Diff", "已获取部分 diff，少数文件未返回 patch"));
                    } else {
                        checks.add(ok("Diff", "已获取可审查 diff"));
                    }
                } else {
                    checks.add(fail("Diff", "未获取到可审查 diff，请确认该提交包含文本代码变更"));
                }
            } catch (RuntimeException ex) {
                checks.add(fail("Commit 内容", ex.getMessage()));
            }
        }

        boolean ready = checks.stream().noneMatch(check -> "FAIL".equals(check.status()));
        return new ReviewPreflightResponse(ready, checks);
    }

    private ReviewPreflightCheckDto ok(String name, String message) {
        return new ReviewPreflightCheckDto(name, "OK", message);
    }

    private ReviewPreflightCheckDto warn(String name, String message) {
        return new ReviewPreflightCheckDto(name, "WARN", message);
    }

    private ReviewPreflightCheckDto fail(String name, String message) {
        return new ReviewPreflightCheckDto(name, "FAIL", message);
    }
}
