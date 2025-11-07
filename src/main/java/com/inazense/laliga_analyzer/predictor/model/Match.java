package com.inazense.laliga_analyzer.predictor.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class Match {
    private String division;
    private LocalDate date;
    private String homeTeam;
    private String awayTeam;
    private Integer fullTimeHomeGoals;
    private Integer fullTimeAwayGoals;
    private String fullTimeResult; // H, D, A
    private Integer halfTimeHomeGoals;
    private Integer halfTimeAwayGoals;
    private String halfTimeResult;
    
    // Match statistics (optional)
    private Integer homeShots;
    private Integer awayShots;
    private Integer homeShotsOnTarget;
    private Integer awayShotsOnTarget;
    private Integer homeCorners;
    private Integer awayCorners;
    private Integer homeFouls;
    private Integer awayFouls;
    private Integer homeYellowCards;
    private Integer awayYellowCards;
    private Integer homeRedCards;
    private Integer awayRedCards;
    
    // Betting odds (optional)
    private Double avgHomeOdds;
    private Double avgDrawOdds;
    private Double avgAwayOdds;
}
