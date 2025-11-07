package com.inazense.laliga_analyzer.predictor.service;

import com.inazense.laliga_analyzer.predictor.model.Match;
import com.inazense.laliga_analyzer.predictor.model.MatchFeatures;
import com.inazense.laliga_analyzer.predictor.model.PredictionResult;
import com.inazense.laliga_analyzer.predictor.model.TeamStats;
import com.inazense.laliga_analyzer.predictor.util.TeamNormalizer;
import com.opencsv.exceptions.CsvException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PredictorService {
    
    private final CsvDataLoader csvDataLoader;
    private final FeatureBuilder featureBuilder;
    private final PoissonModel poissonModel;
    private final ModelStore modelStore;
    
    @Value("${predictor.csvPath:laliga.csv}")
    private String csvPath;
    
    private List<Match> historicalMatches;
    private boolean modelLoaded = false;
    
    @PostConstruct
    public void init() {
        try {
            loadModel();
        } catch (Exception e) {
            log.error("Failed to load model on startup: {}", e.getMessage());
            log.info("Model will need to be trained or loaded manually");
        }
    }
    
    public void loadModel() throws IOException, CsvException {
        log.info("Loading model...");
        
        // Load historical matches for feature building
        historicalMatches = csvDataLoader.loadMatches(csvPath);
        log.info("Loaded {} historical matches", historicalMatches.size());
        
        // Try to load saved model
        try {
            Map<String, TeamStats> teamStatsMap = modelStore.loadTeamStats(modelStore.getModelVersion());
            
            if (teamStatsMap.isEmpty()) {
                log.warn("No saved model found, training new model...");
                trainModel();
            } else {
                // Load team stats into the model
                // For simplicity, we'll retrain if the model is not found
                // In production, you would deserialize the full model state
                log.info("Found saved model, retraining for consistency...");
                trainModel();
            }
        } catch (Exception e) {
            log.warn("Failed to load saved model: {}, training new model...", e.getMessage());
            trainModel();
        }
        
        modelLoaded = true;
        log.info("Model loaded successfully");
    }
    
    private void trainModel() throws IOException, CsvException {
        // Train on all historical data for production use
        // In evaluation mode, we use time-based split
        poissonModel.train(historicalMatches);
        
        // Save the trained model
        Map<String, Object> metadata = Map.of(
                "version", modelStore.getModelVersion(),
                "trainMatches", historicalMatches.size(),
                "trainedAt", LocalDate.now().toString()
        );
        modelStore.saveModel(poissonModel.getTeamStatsMap(), metadata);
    }
    
    public PredictionResult predict(String homeTeam, String awayTeam, LocalDate matchDate) {
        if (!modelLoaded) {
            throw new IllegalStateException("Model not loaded. Please train or load the model first.");
        }
        
        // Normalize team names
        String normalizedHomeTeam = TeamNormalizer.normalize(homeTeam);
        String normalizedAwayTeam = TeamNormalizer.normalize(awayTeam);
        
        // Build features
        MatchFeatures features = featureBuilder.buildFeatures(
                normalizedHomeTeam, 
                normalizedAwayTeam, 
                matchDate, 
                historicalMatches
        );
        
        // Predict
        return poissonModel.predict(
                normalizedHomeTeam, 
                normalizedAwayTeam, 
                features, 
                modelStore.getModelVersion()
        );
    }
    
    public boolean isModelLoaded() {
        return modelLoaded;
    }
}
