package com.inazense.laliga_analyzer.predictor.service;

import com.inazense.laliga_analyzer.predictor.model.Match;
import com.inazense.laliga_analyzer.predictor.model.MatchFeatures;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FeatureBuilder {
    
    @Value("${predictor.windowSize:10}")
    private int windowSize;
    
    @Value("${predictor.minMatchesForFeatures:5}")
    private int minMatchesForFeatures;
    
    public MatchFeatures buildFeatures(String homeTeam, String awayTeam, LocalDate matchDate, List<Match> historicalMatches) {
        // Filter matches before the target date
        List<Match> priorMatches = historicalMatches.stream()
                .filter(m -> m.getDate().isBefore(matchDate))
                .collect(Collectors.toList());
        
        // Get team-specific matches
        List<Match> homeTeamMatches = getTeamMatches(homeTeam, priorMatches);
        List<Match> awayTeamMatches = getTeamMatches(awayTeam, priorMatches);
        List<Match> h2hMatches = getHeadToHeadMatches(homeTeam, awayTeam, priorMatches);
        
        // Get recent matches (last N)
        List<Match> recentHomeMatches = getLastNMatches(homeTeamMatches, windowSize);
        List<Match> recentAwayMatches = getLastNMatches(awayTeamMatches, windowSize);
        
        // Get home/away specific matches
        List<Match> homeTeamHomeMatches = getHomeMatches(homeTeam, recentHomeMatches);
        List<Match> awayTeamAwayMatches = getAwayMatches(awayTeam, recentAwayMatches);
        
        // Calculate features
        MatchFeatures.MatchFeaturesBuilder builder = MatchFeatures.builder()
                .date(matchDate)
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .homeMatchCount(recentHomeMatches.size())
                .awayMatchCount(recentAwayMatches.size())
                .h2hMatchCount(h2hMatches.size());
        
        // Home team overall features
        if (!recentHomeMatches.isEmpty()) {
            builder.homeGoalsScoredAvg(calculateAvgGoalsScored(homeTeam, recentHomeMatches))
                    .homeGoalsConcededAvg(calculateAvgGoalsConceded(homeTeam, recentHomeMatches))
                    .homeWinRate(calculateWinRate(homeTeam, recentHomeMatches))
                    .homeDrawRate(calculateDrawRate(recentHomeMatches))
                    .homeLossRate(calculateLossRate(homeTeam, recentHomeMatches));
        }
        
        // Away team overall features
        if (!recentAwayMatches.isEmpty()) {
            builder.awayGoalsScoredAvg(calculateAvgGoalsScored(awayTeam, recentAwayMatches))
                    .awayGoalsConcededAvg(calculateAvgGoalsConceded(awayTeam, recentAwayMatches))
                    .awayWinRate(calculateWinRate(awayTeam, recentAwayMatches))
                    .awayDrawRate(calculateDrawRate(recentAwayMatches))
                    .awayLossRate(calculateLossRate(awayTeam, recentAwayMatches));
        }
        
        // Home team home form
        if (!homeTeamHomeMatches.isEmpty()) {
            builder.homeTeamHomeGoalsScoredAvg(calculateAvgGoalsScored(homeTeam, homeTeamHomeMatches))
                    .homeTeamHomeGoalsConcededAvg(calculateAvgGoalsConceded(homeTeam, homeTeamHomeMatches))
                    .homeTeamHomeWinRate(calculateWinRate(homeTeam, homeTeamHomeMatches));
        }
        
        // Away team away form
        if (!awayTeamAwayMatches.isEmpty()) {
            builder.awayTeamAwayGoalsScoredAvg(calculateAvgGoalsScored(awayTeam, awayTeamAwayMatches))
                    .awayTeamAwayGoalsConcededAvg(calculateAvgGoalsConceded(awayTeam, awayTeamAwayMatches))
                    .awayTeamAwayWinRate(calculateWinRate(awayTeam, awayTeamAwayMatches));
        }
        
        // Head to head features
        if (!h2hMatches.isEmpty()) {
            builder.h2hHomeWins(calculateH2HHomeWins(homeTeam, awayTeam, h2hMatches))
                    .h2hDraws(calculateH2HDraws(h2hMatches))
                    .h2hAwayWins(calculateH2HAwayWins(homeTeam, awayTeam, h2hMatches))
                    .h2hHomeGoalsAvg(calculateH2HHomeGoalsAvg(homeTeam, awayTeam, h2hMatches))
                    .h2hAwayGoalsAvg(calculateH2HAwayGoalsAvg(homeTeam, awayTeam, h2hMatches));
        }
        
        // Days since last match
        builder.homeDaysSinceLastMatch(calculateDaysSinceLastMatch(homeTeam, matchDate, priorMatches))
                .awayDaysSinceLastMatch(calculateDaysSinceLastMatch(awayTeam, matchDate, priorMatches));
        
        return builder.build();
    }
    
    private List<Match> getTeamMatches(String team, List<Match> matches) {
        return matches.stream()
                .filter(m -> m.getHomeTeam().equals(team) || m.getAwayTeam().equals(team))
                .collect(Collectors.toList());
    }
    
    private List<Match> getHeadToHeadMatches(String team1, String team2, List<Match> matches) {
        return matches.stream()
                .filter(m -> (m.getHomeTeam().equals(team1) && m.getAwayTeam().equals(team2)) ||
                             (m.getHomeTeam().equals(team2) && m.getAwayTeam().equals(team1)))
                .collect(Collectors.toList());
    }
    
    private List<Match> getLastNMatches(List<Match> matches, int n) {
        if (matches.size() <= n) {
            return new ArrayList<>(matches);
        }
        return matches.subList(matches.size() - n, matches.size());
    }
    
    private List<Match> getHomeMatches(String team, List<Match> matches) {
        return matches.stream()
                .filter(m -> m.getHomeTeam().equals(team))
                .collect(Collectors.toList());
    }
    
    private List<Match> getAwayMatches(String team, List<Match> matches) {
        return matches.stream()
                .filter(m -> m.getAwayTeam().equals(team))
                .collect(Collectors.toList());
    }
    
    private double calculateAvgGoalsScored(String team, List<Match> matches) {
        return matches.stream()
                .mapToInt(m -> m.getHomeTeam().equals(team) ? 
                        m.getFullTimeHomeGoals() : m.getFullTimeAwayGoals())
                .average()
                .orElse(0.0);
    }
    
    private double calculateAvgGoalsConceded(String team, List<Match> matches) {
        return matches.stream()
                .mapToInt(m -> m.getHomeTeam().equals(team) ? 
                        m.getFullTimeAwayGoals() : m.getFullTimeHomeGoals())
                .average()
                .orElse(0.0);
    }
    
    private double calculateWinRate(String team, List<Match> matches) {
        long wins = matches.stream()
                .filter(m -> (m.getHomeTeam().equals(team) && "H".equals(m.getFullTimeResult())) ||
                             (m.getAwayTeam().equals(team) && "A".equals(m.getFullTimeResult())))
                .count();
        return (double) wins / matches.size();
    }
    
    private double calculateDrawRate(List<Match> matches) {
        long draws = matches.stream()
                .filter(m -> "D".equals(m.getFullTimeResult()))
                .count();
        return (double) draws / matches.size();
    }
    
    private double calculateLossRate(String team, List<Match> matches) {
        long losses = matches.stream()
                .filter(m -> (m.getHomeTeam().equals(team) && "A".equals(m.getFullTimeResult())) ||
                             (m.getAwayTeam().equals(team) && "H".equals(m.getFullTimeResult())))
                .count();
        return (double) losses / matches.size();
    }
    
    private double calculateH2HHomeWins(String homeTeam, String awayTeam, List<Match> h2hMatches) {
        long wins = h2hMatches.stream()
                .filter(m -> m.getHomeTeam().equals(homeTeam) && "H".equals(m.getFullTimeResult()))
                .count();
        return (double) wins / h2hMatches.size();
    }
    
    private double calculateH2HDraws(List<Match> h2hMatches) {
        long draws = h2hMatches.stream()
                .filter(m -> "D".equals(m.getFullTimeResult()))
                .count();
        return (double) draws / h2hMatches.size();
    }
    
    private double calculateH2HAwayWins(String homeTeam, String awayTeam, List<Match> h2hMatches) {
        long wins = h2hMatches.stream()
                .filter(m -> m.getAwayTeam().equals(awayTeam) && "A".equals(m.getFullTimeResult()))
                .count();
        return (double) wins / h2hMatches.size();
    }
    
    private double calculateH2HHomeGoalsAvg(String homeTeam, String awayTeam, List<Match> h2hMatches) {
        return h2hMatches.stream()
                .filter(m -> m.getHomeTeam().equals(homeTeam))
                .mapToInt(Match::getFullTimeHomeGoals)
                .average()
                .orElse(0.0);
    }
    
    private double calculateH2HAwayGoalsAvg(String homeTeam, String awayTeam, List<Match> h2hMatches) {
        return h2hMatches.stream()
                .filter(m -> m.getAwayTeam().equals(awayTeam))
                .mapToInt(Match::getFullTimeAwayGoals)
                .average()
                .orElse(0.0);
    }
    
    private long calculateDaysSinceLastMatch(String team, LocalDate targetDate, List<Match> matches) {
        return matches.stream()
                .filter(m -> m.getHomeTeam().equals(team) || m.getAwayTeam().equals(team))
                .map(Match::getDate)
                .max(LocalDate::compareTo)
                .map(lastDate -> ChronoUnit.DAYS.between(lastDate, targetDate))
                .orElse(365L); // Default to 365 if no previous match
    }
}
