package com.campus.lostfound.utils;

import com.campus.lostfound.models.Item;
import com.campus.lostfound.models.MatchResult;

import java.util.*;

/**
 * ItemMatchingEngine
 * 
 * Deterministic, explainable matching engine for Smart Campus Lost & Found.
 * Calculates similarity between a target report and eligible opposite-type candidate reports.
 * 
 * Key Principles:
 * 1. Pure Java (No external AI APIs or cloud dependencies).
 * 2. Fully explainable: Every score breakdown is transparent (Category 40%, Location 25%, Text 35%).
 * 3. Never presents score as a probability of identical identity; labeled as "Match Score".
 * 4. Deterministic: Given the same target and candidate data, always produces identical scores.
 */
public class ItemMatchingEngine {

    // Component weights summing to 100.0%
    public static final double CATEGORY_WEIGHT = 40.0;
    public static final double LOCATION_WEIGHT = 25.0;
    public static final double TEXT_WEIGHT = 35.0;

    // Configurable minimum match score threshold to filter out noise
    public static final double DEFAULT_MIN_MATCH_THRESHOLD = 15.0;

    // Standard English stop words to filter out common conversational filler
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are",
            "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but",
            "by", "can", "did", "do", "does", "doing", "don", "down", "during", "each", "few", "for",
            "from", "further", "had", "has", "have", "having", "he", "her", "here", "hers", "herself",
            "him", "himself", "his", "how", "i", "if", "in", "into", "is", "it", "its", "itself",
            "just", "me", "more", "most", "my", "myself", "no", "nor", "not", "now", "of", "off",
            "on", "once", "only", "or", "other", "our", "ours", "ourselves", "out", "over", "own",
            "s", "same", "she", "should", "so", "some", "such", "t", "than", "that", "the", "their",
            "theirs", "them", "themselves", "then", "there", "these", "they", "this", "those", "through",
            "to", "too", "under", "until", "up", "very", "was", "we", "were", "what", "when", "where",
            "which", "while", "who", "whom", "why", "will", "with", "you", "your", "yours", "yourself",
            "yourselves", "lost", "found", "item", "please", "help", "campus", "college"
    );

    /**
     * Finds and ranks candidate matches for a target item from a list of candidates.
     * Uses default minimum score threshold (15.0%).
     *
     * @param targetItem Target item (LOST or FOUND)
     * @param candidates List of potential opposite-type items
     * @return Sorted list of MatchResult objects descending by score
     */
    public static List<MatchResult> findMatches(Item targetItem, List<Item> candidates) {
        return findMatches(targetItem, candidates, DEFAULT_MIN_MATCH_THRESHOLD);
    }

    /**
     * Finds and ranks candidate matches for a target item using a custom minimum score threshold.
     *
     * @param targetItem Target item
     * @param candidates Candidate items
     * @param minThreshold Minimum score to include in results (0.0 to 100.0)
     * @return Sorted list of MatchResult objects descending by score
     */
    public static List<MatchResult> findMatches(Item targetItem, List<Item> candidates, double minThreshold) {
        List<MatchResult> results = new ArrayList<>();
        if (targetItem == null || candidates == null || candidates.isEmpty()) {
            return results;
        }

        for (Item candidate : candidates) {
            // Safety Exclusions:
            // 1. Same item exclusion
            if (candidate.getItemId() == targetItem.getItemId()) {
                continue;
            }
            // 2. Self-reported exclusion (a student cannot match against their own item)
            if (candidate.getReportedBy() == targetItem.getReportedBy()) {
                continue;
            }
            // 3. Same item type exclusion (LOST matches against FOUND only, and vice versa)
            if (candidate.getItemType() == targetItem.getItemType()) {
                continue;
            }
            // 4. Status exclusion: candidate must be OPEN
            if (candidate.getStatus() != Item.ItemStatus.OPEN) {
                continue;
            }

            MatchResult match = calculateMatch(targetItem, candidate);
            if (match.getTotalScore() >= minThreshold) {
                results.add(match);
            }
        }

        // Sort descending by score, then by newest report date
        Collections.sort(results);
        return results;
    }

    /**
     * Calculates the detailed match between two items.
     *
     * @param target Target item
     * @param candidate Candidate item
     * @return MatchResult containing total score and detailed breakdown
     */
    public static MatchResult calculateMatch(Item target, Item candidate) {
        // 1. Category Similarity (Weight 40%)
        double categoryScore = computeCategoryScore(target.getCategory(), candidate.getCategory());

        // 2. Location Similarity (Weight 25%)
        double locationScore = computeLocationScore(target.getLocation(), candidate.getLocation());

        // 3. Text & Keyword Similarity (Weight 35%)
        Set<String> matchedTokens = new LinkedHashSet<>();
        double textScore = computeTextScore(target, candidate, matchedTokens);

        // Build human-readable explainability breakdown
        String explanation = buildExplanation(target, candidate, categoryScore, locationScore, textScore, matchedTokens);

        return new MatchResult(target, candidate, categoryScore, locationScore, textScore, explanation, matchedTokens);
    }

    /**
     * Computes Category similarity score (0.0 to 40.0).
     */
    private static double computeCategoryScore(String targetCat, String candCat) {
        if (targetCat == null || candCat == null) {
            return 0.0;
        }
        String t = targetCat.trim().toLowerCase();
        String c = candCat.trim().toLowerCase();

        if (t.isEmpty() || c.isEmpty()) {
            return 0.0;
        }

        if (t.equals(c)) {
            return CATEGORY_WEIGHT; // 100% of 40 pts
        }

        // Substring / partial match (e.g. "Electronics" vs "Electronic Devices")
        if (t.contains(c) || c.contains(t)) {
            return CATEGORY_WEIGHT * 0.70; // 70% of 40 pts = 28 pts
        }

        return 0.0;
    }

    /**
     * Computes Location similarity score (0.0 to 25.0) using token overlap and Jaccard similarity.
     */
    private static double computeLocationScore(String targetLoc, String candLoc) {
        if (targetLoc == null || candLoc == null) {
            return 0.0;
        }

        String t = targetLoc.trim().toLowerCase();
        String c = candLoc.trim().toLowerCase();

        if (t.isEmpty() || c.isEmpty()) {
            return 0.0;
        }

        if (t.equals(c)) {
            return LOCATION_WEIGHT; // 100% of 25 pts
        }

        Set<String> targetTokens = tokenize(t);
        Set<String> candTokens = tokenize(c);

        if (targetTokens.isEmpty() || candTokens.isEmpty()) {
            return 0.0;
        }

        double jaccard = computeJaccardSimilarity(targetTokens, candTokens);
        return Math.round(jaccard * LOCATION_WEIGHT * 10.0) / 10.0;
    }

    /**
     * Computes Text keyword similarity score (0.0 to 35.0) on title and description.
     */
    private static double computeTextScore(Item target, Item candidate, Set<String> matchedTokensOutput) {
        String targetText = (target.getTitle() != null ? target.getTitle() : "") + " " +
                            (target.getDescription() != null ? target.getDescription() : "");
        String candText = (candidate.getTitle() != null ? candidate.getTitle() : "") + " " +
                          (candidate.getDescription() != null ? candidate.getDescription() : "");

        Set<String> targetTokens = tokenize(targetText);
        Set<String> candTokens = tokenize(candText);

        if (targetTokens.isEmpty() || candTokens.isEmpty()) {
            return 0.0;
        }

        // Intersect tokens
        Set<String> intersection = new LinkedHashSet<>(targetTokens);
        intersection.retainAll(candTokens);

        matchedTokensOutput.addAll(intersection);

        // Calculate Jaccard similarity on Title + Description tokens
        double jaccard = computeJaccardSimilarity(targetTokens, candTokens);

        // Title-specific boost: if key words in target title appear in candidate title
        Set<String> targetTitleTokens = tokenize(target.getTitle());
        Set<String> candTitleTokens = tokenize(candidate.getTitle());
        Set<String> titleIntersection = new LinkedHashSet<>(targetTitleTokens);
        titleIntersection.retainAll(candTitleTokens);

        double baseScore = jaccard * TEXT_WEIGHT;

        // If title keywords matched directly, apply a boost
        if (!titleIntersection.isEmpty()) {
            double titleOverlapRatio = (double) titleIntersection.size() / Math.max(1, targetTitleTokens.size());
            baseScore = Math.max(baseScore, titleOverlapRatio * TEXT_WEIGHT * 0.85);
        }

        return Math.round(Math.min(TEXT_WEIGHT, baseScore) * 10.0) / 10.0;
    }

    /**
     * Computes Jaccard Similarity: |A ∩ B| / |A ∪ B|.
     */
    public static double computeJaccardSimilarity(Set<String> setA, Set<String> setB) {
        if (setA == null || setB == null || setA.isEmpty() || setB.isEmpty()) {
            return 0.0;
        }

        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);

        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / (double) union.size();
    }

    /**
     * Tokenizes text: converts to lowercase, strips punctuation, splits whitespace, and drops stop words.
     */
    public static Set<String> tokenize(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        if (text == null) {
            return tokens;
        }

        // Remove punctuation and special characters, keep alphanumeric and spaces
        String cleaned = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").trim();
        String[] words = cleaned.split("\\s+");

        for (String w : words) {
            w = w.trim();
            // Drop short tokens (< 2 chars) and stop words
            if (w.length() >= 2 && !STOP_WORDS.contains(w)) {
                tokens.add(w);
            }
        }
        return tokens;
    }

    /**
     * Builds a transparent human-readable explanation of the score components.
     */
    private static String buildExplanation(Item target, Item candidate, double catScore,
                                           double locScore, double txtScore, Set<String> matchedTokens) {
        StringBuilder sb = new StringBuilder();

        // Category breakdown
        sb.append(String.format("Category: %.0f/%.0f pts", catScore, CATEGORY_WEIGHT));
        if (catScore >= CATEGORY_WEIGHT * 0.99) {
            sb.append(" (Exact: '").append(candidate.getCategory()).append("')");
        } else if (catScore > 0) {
            sb.append(" (Partial match)");
        } else {
            sb.append(" (Different category)");
        }

        // Location breakdown
        sb.append(String.format(" | Location: %.0f/%.0f pts", locScore, LOCATION_WEIGHT));
        if (locScore >= LOCATION_WEIGHT * 0.99) {
            sb.append(" (Same spot: '").append(candidate.getLocation()).append("')");
        } else if (locScore > 0) {
            sb.append(" (Location keyword overlap)");
        } else {
            sb.append(" (Different campus location)");
        }

        // Text breakdown
        sb.append(String.format(" | Keywords: %.0f/%.0f pts", txtScore, TEXT_WEIGHT));
        if (!matchedTokens.isEmpty()) {
            sb.append(" [Matched: ");
            int count = 0;
            for (String tok : matchedTokens) {
                if (count > 0) sb.append(", ");
                sb.append("'").append(tok).append("'");
                count++;
                if (count >= 4) {
                    if (matchedTokens.size() > 4) sb.append(", +").append(matchedTokens.size() - 4).append(" more");
                    break;
                }
            }
            sb.append("]");
        } else {
            sb.append(" (No shared keywords)");
        }

        return sb.toString();
    }
}
