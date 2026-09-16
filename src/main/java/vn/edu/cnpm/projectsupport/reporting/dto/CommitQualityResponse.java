package vn.edu.cnpm.projectsupport.reporting.dto;

public record CommitQualityResponse(
        Long memberId, String username, long commits, long additions, long deletions,
        long filesChanged, long revertedCommits, long successfulChecks, long failedChecks,
        double checkSuccessRate, double qualityScore, String rating) {}
