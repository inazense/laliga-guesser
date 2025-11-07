package com.inazense.laliga_analyzer.predictor.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeamStats {
    private String teamName;
    private double attackStrength;
    private double defenseStrength;
    private double homeAdvantage;
    private int matchesPlayed;
    private int goalsScored;
    private int goalsConceded;
    private int wins;
    private int draws;
    private int losses;
}
