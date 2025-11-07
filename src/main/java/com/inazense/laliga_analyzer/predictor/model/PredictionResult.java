package com.inazense.laliga_analyzer.predictor.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class PredictionResult {
    private ProbabilityOutcome probabilities;
    private List<ScorePrediction> topScores;
    private Prediction prediction;
    private List<FeatureImportance> explain;
    private String modelVersion;
    
    @Data
    @Builder
    public static class ProbabilityOutcome {
        private double homeWin;
        private double draw;
        private double awayWin;
    }
    
    @Data
    @Builder
    public static class Prediction {
        private String outcome; // "homeWin", "draw", "awayWin"
        private double confidence;
    }
    
    @Data
    @Builder
    public static class FeatureImportance {
        private String feature;
        private double impact;
    }
}
