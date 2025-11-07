package com.inazense.laliga_analyzer.predictor.dto;

import lombok.Data;

@Data
public class PredictionRequest {
    private String date;
    private String homeTeam;
    private String awayTeam;
    private MatchContext context;
    
    @Data
    public static class MatchContext {
        private Integer matchday;
        private String competition;
    }
}
