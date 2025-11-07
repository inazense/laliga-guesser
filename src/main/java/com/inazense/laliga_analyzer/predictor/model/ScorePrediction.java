package com.inazense.laliga_analyzer.predictor.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScorePrediction {
    private String score;
    private double prob;
}
