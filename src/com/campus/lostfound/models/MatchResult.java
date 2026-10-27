package com.campus.lostfound.models;

import java.util.Set;

/**
 * MatchResult Model
 * 
 * Represents an explainable similarity match between a target item and an eligible candidate item.
 * Encapsulates the overall Match Score (0 to 100) and its constituent breakdown:
 * - Category score (max 40 pts)
 * - Location score (max 25 pts)
 * - Text / keyword similarity score (max 35 pts)
 * Includes human-readable explanations and matched keyword tokens.
 */
public class MatchResult implements Comparable<MatchResult> {

    private final Item targetItem;
    private final Item candidateItem;
    private final double totalScore;
    private final double categoryScore;
    private final double locationScore;
    private final double textScore;
    private final String explanation;
    private final Set<String> matchedKeywords;

    public MatchResult(Item targetItem, Item candidateItem, double categoryScore,
                       double locationScore, double textScore, String explanation,
                       Set<String> matchedKeywords) {
        this.targetItem = targetItem;
        this.candidateItem = candidateItem;
        this.categoryScore = Math.max(0.0, Math.min(40.0, categoryScore));
        this.locationScore = Math.max(0.0, Math.min(25.0, locationScore));
        this.textScore = Math.max(0.0, Math.min(35.0, textScore));
        double rawTotal = this.categoryScore + this.locationScore + this.textScore;
        this.totalScore = Math.round(Math.max(0.0, Math.min(100.0, rawTotal)) * 10.0) / 10.0;
        this.explanation = explanation != null ? explanation : "";
        this.matchedKeywords = matchedKeywords != null ? matchedKeywords : Set.of();
    }

    public Item getTargetItem() {
        return targetItem;
    }

    public Item getCandidateItem() {
        return candidateItem;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public double getCategoryScore() {
        return categoryScore;
    }

    public double getLocationScore() {
        return locationScore;
    }

    public double getTextScore() {
        return textScore;
    }

    public String getExplanation() {
        return explanation;
    }

    public Set<String> getMatchedKeywords() {
        return matchedKeywords;
    }

    public String getFormattedScore() {
        return String.format("%.0f%%", totalScore);
    }

    public String getMatchConfidenceTier() {
        if (totalScore >= 75.0) {
            return "Strong Match";
        } else if (totalScore >= 50.0) {
            return "Moderate Match";
        } else if (totalScore >= 30.0) {
            return "Possible Match";
        } else {
            return "Low Match";
        }
    }

    @Override
    public int compareTo(MatchResult other) {
        // Sort descending by total score
        int scoreCompare = Double.compare(other.totalScore, this.totalScore);
        if (scoreCompare != 0) {
            return scoreCompare;
        }
        // Tie-breaker: Newer candidate items first
        if (this.candidateItem != null && other.candidateItem != null
                && this.candidateItem.getReportDate() != null && other.candidateItem.getReportDate() != null) {
            return other.candidateItem.getReportDate().compareTo(this.candidateItem.getReportDate());
        }
        return 0;
    }

    @Override
    public String toString() {
        return String.format("MatchResult[Candidate #%d '%s', Score=%.1f%% (%s)]",
                candidateItem != null ? candidateItem.getItemId() : 0,
                candidateItem != null ? candidateItem.getTitle() : "N/A",
                totalScore,
                getMatchConfidenceTier());
    }
}
