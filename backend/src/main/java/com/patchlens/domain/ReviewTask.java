package com.patchlens.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "review_tasks")
public class ReviewTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private RepositoryConnection repository;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepositoryProvider provider;

    @Column(nullable = false)
    private int prNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewTargetType targetType = ReviewTargetType.PULL_REQUEST;

    @Column(nullable = false)
    private String commitSha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status = ReviewStatus.QUEUED;

    @Enumerated(EnumType.STRING)
    private ReviewConclusion conclusion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TriggerType triggerType = TriggerType.WEBHOOK;

    @Column(columnDefinition = "text")
    private String summary;

    private Instant startedAt;

    private Instant finishedAt;

    @Column(columnDefinition = "text")
    private String errorMessage;

    private Instant publishedAt;

    @Column(columnDefinition = "text")
    private String publishError;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public RepositoryConnection getRepository() {
        return repository;
    }

    public void setRepository(RepositoryConnection repository) {
        this.repository = repository;
    }

    public RepositoryProvider getProvider() {
        return provider;
    }

    public void setProvider(RepositoryProvider provider) {
        this.provider = provider;
    }

    public int getPrNumber() {
        return prNumber;
    }

    public void setPrNumber(int prNumber) {
        this.prNumber = prNumber;
    }

    public ReviewTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(ReviewTargetType targetType) {
        this.targetType = targetType;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public ReviewConclusion getConclusion() {
        return conclusion;
    }

    public void setConclusion(ReviewConclusion conclusion) {
        this.conclusion = conclusion;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getPublishError() {
        return publishError;
    }

    public void setPublishError(String publishError) {
        this.publishError = publishError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
