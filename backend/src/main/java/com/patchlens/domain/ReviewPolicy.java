package com.patchlens.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "review_policies")
public class ReviewPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private RepositoryConnection repository;

    @Column(nullable = false)
    private String language = "zh-CN";

    @Column(nullable = false)
    private double minConfidence = 0.75;

    @Column(nullable = false)
    private int maxInlineComments = 5;

    @Column(nullable = false)
    private boolean enableSummary = true;

    @Column(nullable = false)
    private boolean enableInlineComments = true;

    @Column(nullable = false)
    private boolean enableSuggestedPatch = true;

    @Column(columnDefinition = "text")
    private String ignoredPaths = "";

    @Column(columnDefinition = "text")
    private String focusPaths = "";

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public RepositoryConnection getRepository() {
        return repository;
    }

    public void setRepository(RepositoryConnection repository) {
        this.repository = repository;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public double getMinConfidence() {
        return minConfidence;
    }

    public void setMinConfidence(double minConfidence) {
        this.minConfidence = minConfidence;
    }

    public int getMaxInlineComments() {
        return maxInlineComments;
    }

    public void setMaxInlineComments(int maxInlineComments) {
        this.maxInlineComments = maxInlineComments;
    }

    public boolean isEnableSummary() {
        return enableSummary;
    }

    public void setEnableSummary(boolean enableSummary) {
        this.enableSummary = enableSummary;
    }

    public boolean isEnableInlineComments() {
        return enableInlineComments;
    }

    public void setEnableInlineComments(boolean enableInlineComments) {
        this.enableInlineComments = enableInlineComments;
    }

    public boolean isEnableSuggestedPatch() {
        return enableSuggestedPatch;
    }

    public void setEnableSuggestedPatch(boolean enableSuggestedPatch) {
        this.enableSuggestedPatch = enableSuggestedPatch;
    }

    public String getIgnoredPaths() {
        return ignoredPaths;
    }

    public void setIgnoredPaths(String ignoredPaths) {
        this.ignoredPaths = ignoredPaths;
    }

    public String getFocusPaths() {
        return focusPaths;
    }

    public void setFocusPaths(String focusPaths) {
        this.focusPaths = focusPaths;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markUpdated() {
        this.updatedAt = Instant.now();
    }
}
