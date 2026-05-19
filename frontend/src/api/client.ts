import axios from 'axios';

export const api = axios.create({
  baseURL: '/api',
  timeout: 120_000,
  withCredentials: true,
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401 && window.location.pathname !== '/login') {
      window.location.href = '/login';
    }
    return Promise.reject(error);
  },
);

export function getApiErrorMessage(error: unknown, fallback: string) {
  if (!axios.isAxiosError(error)) {
    return fallback;
  }
  if (error.code === 'ECONNABORTED') {
    return '审查请求超时。模型审查可能还在后台执行，请稍后刷新审查记录。';
  }
  const data = error.response?.data;
  if (typeof data === 'string' && data.trim()) {
    return data;
  }
  if (data && typeof data === 'object') {
    const payload = data as { error?: string; errorMessage?: string; message?: string };
    return payload.errorMessage || payload.message || payload.error || fallback;
  }
  return error.message || fallback;
}

export interface Repository {
  id: number;
  provider: string;
  owner: string;
  name: string;
  defaultBranch: string;
  enabled: boolean;
}

export interface RepositoryCommit {
  sha: string;
  shortSha: string;
  message: string;
  authorName: string;
  authoredAt: string;
  webUrl: string;
}

export interface RepositoryBranch {
  name: string;
  latestCommitSha: string;
}

export interface ReviewTask {
  id: number;
  repositoryId: number;
  repositoryName: string;
  targetType: string;
  prNumber: number;
  commitSha: string;
  status: string;
  conclusion: string;
  triggerType: string;
  summary: string | null;
  errorMessage: string | null;
  publishError: string | null;
  startedAt: string | null;
  finishedAt: string | null;
  publishedAt: string | null;
  createdAt: string;
}

export interface ReviewPreflightCheck {
  name: string;
  status: string;
  message: string;
}

export interface ReviewPreflightResponse {
  ready: boolean;
  checks: ReviewPreflightCheck[];
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ReviewGovernance {
  queued: number;
  running: number;
  completed: number;
  failed: number;
  canceled: number;
  stuck: number;
  stuckMinutes: number;
}

export interface ReviewFinding {
  id: number;
  severity: string;
  confidence: number;
  filePath: string;
  lineNumber: number;
  title: string;
  description: string | null;
  evidence: string | null;
  suggestion: string | null;
  patch: string | null;
  published: boolean;
  feedbackCounts: Record<string, number>;
  latestFeedbackValue: string;
  latestFeedbackNote: string | null;
  latestFeedbackAt: string | null;
}

export interface ReviewFeedback {
  id: number;
  findingId: number;
  value: string;
  note: string | null;
  createdAt: string;
}

export interface ModelConfig {
  id: number;
  provider: string;
  baseUrl: string;
  modelName: string;
  hasApiKey: boolean;
  enabled: boolean;
}

export interface ModelConfigTestResult {
  success: boolean;
  message: string;
  latencyMs: number;
}

export interface ModelOption {
  id: string;
  ownedBy: string;
}

export interface PlatformConfig {
  id: number;
  provider: string;
  apiBaseUrl: string;
  hasAccessToken: boolean;
  hasWebhookSecret: boolean;
  enabled: boolean;
}

export interface PlatformCapability {
  provider: string;
  displayName: string;
  configurable: boolean;
  configured: boolean;
  enabled: boolean;
  apiBaseUrl: string;
  hasAccessToken: boolean;
  hasWebhookSecret: boolean;
  repositoryCount: number;
  enabledRepositoryCount: number;
  supportsRecentCommits: boolean;
  supportsCommitReview: boolean;
  supportsPullRequestReview: boolean;
  supportsWebhook: boolean;
  supportsInlineComments: boolean;
  supportsSuggestedPatch: boolean;
  status: string;
  gaps: string[];
}

export interface PlatformCapabilitySummary {
  repositoryCount: number;
  enabledRepositoryCount: number;
  configuredPlatformCount: number;
  activeModelReady: boolean;
  activeModelName: string;
  capabilities: PlatformCapability[];
}

export interface ReviewPolicy {
  id: number;
  repositoryId: number;
  language: string;
  minConfidence: number;
  maxInlineComments: number;
  enableSummary: boolean;
  enableInlineComments: boolean;
  enableSuggestedPatch: boolean;
  ignoredPaths: string;
  focusPaths: string;
}

export interface AuthUser {
  authenticated: boolean;
  username: string;
  displayName: string;
  role: string;
}

export interface UserAccount {
  id: number;
  username: string;
  displayName: string;
  role: string;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}
