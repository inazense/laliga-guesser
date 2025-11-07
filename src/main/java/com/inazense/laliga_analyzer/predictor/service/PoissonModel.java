package com.inazense.laliga_analyzer.predictor.service;

import com.inazense.laliga_analyzer.predictor.model.Match;
import com.inazense.laliga_analyzer.predictor.model.MatchFeatures;
import com.inazense.laliga_analyzer.predictor.model.PredictionResult;
import com.inazense.laliga_analyzer.predictor.model.ScorePrediction;
import com.inazense.laliga_analyzer.predictor.model.TeamStats;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PoissonModel {
    
    private Map<String, TeamStats> teamStatsMap;
    private double leagueAvgGoals;
    private double homeAdvantage;
    
    public void train(List<Match> trainingMatches) {
        teamStatsMap = new HashMap<>();
        
        // Calculate overall league statistics
        int totalGoals = 0;
        int totalMatches = trainingMatches.size();
        int totalHomeWins = 0;
        
        // First pass: collect basic stats
        for (Match match : trainingMatches) {
            totalGoals += match.getFullTimeHomeGoals() + match.getFullTimeAwayGoals();
            if ("H".equals(match.getFullTimeResult())) {
                totalHomeWins++;
            }
            
            updateTeamStats(match.getHomeTeam(), match, true);
            updateTeamStats(match.getAwayTeam(), match, false);
        }
        
        leagueAvgGoals = (double) totalGoals / (totalMatches * 2);
        homeAdvantage = (double) totalHomeWins / totalMatches;
        
        // Second pass: calculate attack and defense strengths
        for (TeamStats stats : teamStatsMap.values()) {
            if (stats.getMatchesPlayed() > 0) {
                double avgGoalsScored = (double) stats.getGoalsScored() / stats.getMatchesPlayed();
                double avgGoalsConceded = (double) stats.getGoalsConceded() / stats.getMatchesPlayed();
                
                stats.setAttackStrength(avgGoalsScored / leagueAvgGoals);
                stats.setDefenseStrength(avgGoalsConceded / leagueAvgGoals);
            }
        }
        
        log.info("Trained Poisson model with {} teams, league avg goals: {}, home advantage: {}", 
                teamStatsMap.size(), leagueAvgGoals, homeAdvantage);
    }
    
    private void updateTeamStats(String teamName, Match match, boolean isHome) {
        TeamStats stats = teamStatsMap.computeIfAbsent(teamName, k -> TeamStats.builder()
                .teamName(teamName)
                .attackStrength(1.0)
                .defenseStrength(1.0)
                .homeAdvantage(1.15) // Default home advantage multiplier
                .matchesPlayed(0)
                .goalsScored(0)
                .goalsConceded(0)
                .wins(0)
                .draws(0)
                .losses(0)
                .build());
        
        stats.setMatchesPlayed(stats.getMatchesPlayed() + 1);
        
        if (isHome) {
            stats.setGoalsScored(stats.getGoalsScored() + match.getFullTimeHomeGoals());
            stats.setGoalsConceded(stats.getGoalsConceded() + match.getFullTimeAwayGoals());
            
            if ("H".equals(match.getFullTimeResult())) {
                stats.setWins(stats.getWins() + 1);
            } else if ("D".equals(match.getFullTimeResult())) {
                stats.setDraws(stats.getDraws() + 1);
            } else {
                stats.setLosses(stats.getLosses() + 1);
            }
        } else {
            stats.setGoalsScored(stats.getGoalsScored() + match.getFullTimeAwayGoals());
            stats.setGoalsConceded(stats.getGoalsConceded() + match.getFullTimeHomeGoals());
            
            if ("A".equals(match.getFullTimeResult())) {
                stats.setWins(stats.getWins() + 1);
            } else if ("D".equals(match.getFullTimeResult())) {
                stats.setDraws(stats.getDraws() + 1);
            } else {
                stats.setLosses(stats.getLosses() + 1);
            }
        }
    }
    
    public PredictionResult predict(String homeTeam, String awayTeam, MatchFeatures features, String modelVersion) {
        TeamStats homeStats = teamStatsMap.get(homeTeam);
        TeamStats awayStats = teamStatsMap.get(awayTeam);
        
        if (homeStats == null || awayStats == null) {
            log.warn("Team stats not found for {} vs {}", homeTeam, awayTeam);
            return createDefaultPrediction(modelVersion);
        }
        
        // Calculate expected goals using Poisson model
        // λ_home = home_attack * away_defense * home_advantage * league_avg
        // λ_away = away_attack * home_defense * league_avg
        double homeExpectedGoals = homeStats.getAttackStrength() * 
                                   awayStats.getDefenseStrength() * 
                                   homeStats.getHomeAdvantage() * 
                                   leagueAvgGoals;
        
        double awayExpectedGoals = awayStats.getAttackStrength() * 
                                   homeStats.getDefenseStrength() * 
                                   leagueAvgGoals;
        
        // Adjust with recent form if features are available
        if (features != null && features.getHomeMatchCount() >= 5 && features.getAwayMatchCount() >= 5) {
            double homeFormFactor = (features.getHomeGoalsScoredAvg() + features.getHomeTeamHomeGoalsScoredAvg()) / 
                                    (2 * leagueAvgGoals);
            double awayFormFactor = (features.getAwayGoalsScoredAvg() + features.getAwayTeamAwayGoalsScoredAvg()) / 
                                    (2 * leagueAvgGoals);
            
            homeExpectedGoals = 0.7 * homeExpectedGoals + 0.3 * homeExpectedGoals * homeFormFactor;
            awayExpectedGoals = 0.7 * awayExpectedGoals + 0.3 * awayExpectedGoals * awayFormFactor;
        }
        
        // Calculate probabilities using Poisson distribution
        Map<String, Double> scoreProbabilities = calculateScoreProbabilities(homeExpectedGoals, awayExpectedGoals);
        
        // Calculate outcome probabilities
        double homeWinProb = 0.0;
        double drawProb = 0.0;
        double awayWinProb = 0.0;
        
        for (Map.Entry<String, Double> entry : scoreProbabilities.entrySet()) {
            String[] scoreParts = entry.getKey().split("-");
            int homeGoals = Integer.parseInt(scoreParts[0]);
            int awayGoals = Integer.parseInt(scoreParts[1]);
            double prob = entry.getValue();
            
            if (homeGoals > awayGoals) {
                homeWinProb += prob;
            } else if (homeGoals == awayGoals) {
                drawProb += prob;
            } else {
                awayWinProb += prob;
            }
        }
        
        // Normalize probabilities
        double total = homeWinProb + drawProb + awayWinProb;
        homeWinProb /= total;
        drawProb /= total;
        awayWinProb /= total;
        
        // Get top 3 most likely scores
        List<ScorePrediction> topScores = scoreProbabilities.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .map(e -> ScorePrediction.builder()
                        .score(e.getKey())
                        .prob(e.getValue())
                        .build())
                .collect(Collectors.toList());
        
        // Determine prediction
        String outcome;
        double confidence;
        if (homeWinProb > drawProb && homeWinProb > awayWinProb) {
            outcome = "homeWin";
            confidence = homeWinProb;
        } else if (awayWinProb > drawProb && awayWinProb > homeWinProb) {
            outcome = "awayWin";
            confidence = awayWinProb;
        } else {
            outcome = "draw";
            confidence = drawProb;
        }
        
        // Build feature importance (for explainability)
        List<PredictionResult.FeatureImportance> featureImportance = new ArrayList<>();
        featureImportance.add(PredictionResult.FeatureImportance.builder()
                .feature("home_attack_strength")
                .impact(homeStats.getAttackStrength() - 1.0)
                .build());
        featureImportance.add(PredictionResult.FeatureImportance.builder()
                .feature("away_attack_strength")
                .impact(awayStats.getAttackStrength() - 1.0)
                .build());
        featureImportance.add(PredictionResult.FeatureImportance.builder()
                .feature("home_defense_strength")
                .impact(1.0 - homeStats.getDefenseStrength())
                .build());
        featureImportance.add(PredictionResult.FeatureImportance.builder()
                .feature("away_defense_strength")
                .impact(1.0 - awayStats.getDefenseStrength())
                .build());
        
        if (features != null && features.getHomeMatchCount() >= 5) {
            featureImportance.add(PredictionResult.FeatureImportance.builder()
                    .feature("home_recent_form")
                    .impact(features.getHomeWinRate() - 0.33)
                    .build());
        }
        
        if (features != null && features.getAwayMatchCount() >= 5) {
            featureImportance.add(PredictionResult.FeatureImportance.builder()
                    .feature("away_recent_form")
                    .impact(features.getAwayWinRate() - 0.33)
                    .build());
        }
        
        return PredictionResult.builder()
                .probabilities(PredictionResult.ProbabilityOutcome.builder()
                        .homeWin(homeWinProb)
                        .draw(drawProb)
                        .awayWin(awayWinProb)
                        .build())
                .topScores(topScores)
                .prediction(PredictionResult.Prediction.builder()
                        .outcome(outcome)
                        .confidence(confidence)
                        .build())
                .explain(featureImportance)
                .modelVersion(modelVersion)
                .build();
    }
    
    private Map<String, Double> calculateScoreProbabilities(double homeExpectedGoals, double awayExpectedGoals) {
        Map<String, Double> probabilities = new HashMap<>();
        
        PoissonDistribution homePoisson = new PoissonDistribution(homeExpectedGoals);
        PoissonDistribution awayPoisson = new PoissonDistribution(awayExpectedGoals);
        
        // Calculate probabilities for scores up to 6-6 (covers most realistic scenarios)
        for (int homeGoals = 0; homeGoals <= 6; homeGoals++) {
            for (int awayGoals = 0; awayGoals <= 6; awayGoals++) {
                double prob = homePoisson.probability(homeGoals) * awayPoisson.probability(awayGoals);
                probabilities.put(homeGoals + "-" + awayGoals, prob);
            }
        }
        
        return probabilities;
    }
    
    private PredictionResult createDefaultPrediction(String modelVersion) {
        return PredictionResult.builder()
                .probabilities(PredictionResult.ProbabilityOutcome.builder()
                        .homeWin(0.33)
                        .draw(0.33)
                        .awayWin(0.33)
                        .build())
                .topScores(List.of(
                        ScorePrediction.builder().score("1-1").prob(0.15).build(),
                        ScorePrediction.builder().score("1-0").prob(0.12).build(),
                        ScorePrediction.builder().score("0-1").prob(0.12).build()
                ))
                .prediction(PredictionResult.Prediction.builder()
                        .outcome("draw")
                        .confidence(0.33)
                        .build())
                .explain(new ArrayList<>())
                .modelVersion(modelVersion)
                .build();
    }
    
    public Map<String, TeamStats> getTeamStatsMap() {
        return teamStatsMap;
    }
}
