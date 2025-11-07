package com.inazense.laliga_analyzer.predictor.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class MatchFeatures {
    private LocalDate date;
    private String homeTeam;
    private String awayTeam;
    
    // Home team features (last N games)
    private double homeGoalsScoredAvg;
    private double homeGoalsConcededAvg;
    private double homeWinRate;
    private double homeDrawRate;
    private double homeLossRate;
    
    // Away team features (last N games)
    private double awayGoalsScoredAvg;
    private double awayGoalsConcededAvg;
    private double awayWinRate;
    private double awayDrawRate;
    private double awayLossRate;
    
    // Home team form at home (last N home games)
    private double homeTeamHomeGoalsScoredAvg;
    private double homeTeamHomeGoalsConcededAvg;
    private double homeTeamHomeWinRate;
    
    // Away team form away (last N away games)
    private double awayTeamAwayGoalsScoredAvg;
    private double awayTeamAwayGoalsConcededAvg;
    private double awayTeamAwayWinRate;
    
    // Head to head
    private double h2hHomeWins;
    private double h2hDraws;
    private double h2hAwayWins;
    private double h2hHomeGoalsAvg;
    private double h2hAwayGoalsAvg;
    
    // Days since last match
    private long homeDaysSinceLastMatch;
    private long awayDaysSinceLastMatch;
    
    // Betting odds (if available)
    private Double avgHomeOdds;
    private Double avgDrawOdds;
    private Double avgAwayOdds;
    
    // Number of matches used for features
    private int homeMatchCount;
    private int awayMatchCount;
    private int h2hMatchCount;
}
