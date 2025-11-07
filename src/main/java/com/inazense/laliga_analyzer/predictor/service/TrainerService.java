package com.inazense.laliga_analyzer.predictor.service;

import com.inazense.laliga_analyzer.predictor.model.Match;
import com.inazense.laliga_analyzer.predictor.model.MatchFeatures;
import com.inazense.laliga_analyzer.predictor.model.PredictionResult;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainerService {
    
    private final CsvDataLoader csvDataLoader;
    private final FeatureBuilder featureBuilder;
    private final PoissonModel poissonModel;
    private final ModelStore modelStore;
    
    @Value("${predictor.csvPath:laliga.csv}")
    private String csvPath;
    
    @Value("${predictor.trainTestSplitYear:2023}")
    private int trainTestSplitYear;
    
    public Map<String, Object> trainAndEvaluate() throws IOException, CsvException {
        log.info("Starting training and evaluation...");
        
        // Load all matches
        List<Match> allMatches = csvDataLoader.loadMatches(csvPath);
        log.info("Loaded {} total matches", allMatches.size());
        
        // Split by date (temporal split)
        LocalDate splitDate = LocalDate.of(trainTestSplitYear, 1, 1);
        List<Match> trainMatches = allMatches.stream()
                .filter(m -> m.getDate().isBefore(splitDate))
                .collect(Collectors.toList());
        
        List<Match> testMatches = allMatches.stream()
                .filter(m -> !m.getDate().isBefore(splitDate))
                .collect(Collectors.toList());
        
        log.info("Training set: {} matches (before {})", trainMatches.size(), splitDate);
        log.info("Test set: {} matches (from {} onwards)", testMatches.size(), splitDate);
        
        // Train model
        poissonModel.train(trainMatches);
        
        // Evaluate on test set
        Map<String, Object> metrics = evaluate(testMatches, allMatches);
        
        // Save model
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("version", modelStore.getModelVersion());
        metadata.put("trainMatches", trainMatches.size());
        metadata.put("testMatches", testMatches.size());
        metadata.put("splitDate", splitDate.toString());
        metadata.put("metrics", metrics);
        
        modelStore.saveModel(poissonModel.getTeamStatsMap(), metadata);
        
        log.info("Training and evaluation completed successfully");
        return metrics;
    }
    
    private Map<String, Object> evaluate(List<Match> testMatches, List<Match> allMatches) {
        log.info("Evaluating model on {} test matches...", testMatches.size());
        
        int correct = 0;
        int total = 0;
        double brierScore = 0.0;
        double logLoss = 0.0;
        
        Map<String, Integer> confusionMatrix = new HashMap<>();
        confusionMatrix.put("HH", 0); // Predicted Home, Actual Home
        confusionMatrix.put("HD", 0);
        confusionMatrix.put("HA", 0);
        confusionMatrix.put("DH", 0);
        confusionMatrix.put("DD", 0);
        confusionMatrix.put("DA", 0);
        confusionMatrix.put("AH", 0);
        confusionMatrix.put("AD", 0);
        confusionMatrix.put("AA", 0);
        
        for (Match match : testMatches) {
            try {
                // Build features from historical data (before this match)
                MatchFeatures features = featureBuilder.buildFeatures(
                        match.getHomeTeam(), 
                        match.getAwayTeam(), 
                        match.getDate(), 
                        allMatches
                );
                
                // Predict
                PredictionResult prediction = poissonModel.predict(
                        match.getHomeTeam(), 
                        match.getAwayTeam(), 
                        features, 
                        modelStore.getModelVersion()
                );
                
                // Get actual result
                String actualResult = match.getFullTimeResult();
                String predictedResult = outcomeToResult(prediction.getPrediction().getOutcome());
                
                // Update confusion matrix
                String key = predictedResult + actualResult;
                confusionMatrix.put(key, confusionMatrix.get(key) + 1);
                
                // Check if prediction is correct
                if (predictedResult.equals(actualResult)) {
                    correct++;
                }
                total++;
                
                // Calculate Brier score
                double homeActual = "H".equals(actualResult) ? 1.0 : 0.0;
                double drawActual = "D".equals(actualResult) ? 1.0 : 0.0;
                double awayActual = "A".equals(actualResult) ? 1.0 : 0.0;
                
                brierScore += Math.pow(prediction.getProbabilities().getHomeWin() - homeActual, 2);
                brierScore += Math.pow(prediction.getProbabilities().getDraw() - drawActual, 2);
                brierScore += Math.pow(prediction.getProbabilities().getAwayWin() - awayActual, 2);
                
                // Calculate log loss
                double epsilon = 1e-15; // To avoid log(0)
                if ("H".equals(actualResult)) {
                    logLoss -= Math.log(Math.max(prediction.getProbabilities().getHomeWin(), epsilon));
                } else if ("D".equals(actualResult)) {
                    logLoss -= Math.log(Math.max(prediction.getProbabilities().getDraw(), epsilon));
                } else {
                    logLoss -= Math.log(Math.max(prediction.getProbabilities().getAwayWin(), epsilon));
                }
                
            } catch (Exception e) {
                log.warn("Failed to evaluate match {} vs {}: {}", 
                        match.getHomeTeam(), match.getAwayTeam(), e.getMessage());
            }
        }
        
        double accuracy = (double) correct / total;
        brierScore = brierScore / (3 * total); // Normalize by number of classes and samples
        logLoss = logLoss / total;
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("accuracy", accuracy);
        metrics.put("brierScore", brierScore);
        metrics.put("logLoss", logLoss);
        metrics.put("totalPredictions", total);
        metrics.put("correctPredictions", correct);
        metrics.put("confusionMatrix", confusionMatrix);
        
        log.info("Evaluation metrics: Accuracy={}, Brier Score={}, Log Loss={}", 
                accuracy, brierScore, logLoss);
        
        return metrics;
    }
    
    private String outcomeToResult(String outcome) {
        return switch (outcome) {
            case "homeWin" -> "H";
            case "draw" -> "D";
            case "awayWin" -> "A";
            default -> "D"; // Default to draw
        };
    }
}
