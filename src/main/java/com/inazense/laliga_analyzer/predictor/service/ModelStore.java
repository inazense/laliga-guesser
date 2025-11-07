package com.inazense.laliga_analyzer.predictor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inazense.laliga_analyzer.predictor.model.TeamStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ModelStore {
    
    @Value("${predictor.modelsPath:models}")
    private String modelsPath;
    
    @Value("${predictor.modelVersion:v0.1}")
    private String modelVersion;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public void saveModel(Map<String, TeamStats> teamStatsMap, Map<String, Object> metadata) throws IOException {
        // Create models directory if it doesn't exist
        Path modelsDir = Paths.get(modelsPath);
        if (!Files.exists(modelsDir)) {
            Files.createDirectories(modelsDir);
        }
        
        // Save team stats
        String teamStatsFilename = String.format("%s/team_stats_%s.json", modelsPath, modelVersion);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(teamStatsFilename), teamStatsMap);
        log.info("Saved team stats to {}", teamStatsFilename);
        
        // Save metadata
        String metadataFilename = String.format("%s/metadata_%s.json", modelsPath, modelVersion);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(metadataFilename), metadata);
        log.info("Saved metadata to {}", metadataFilename);
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, TeamStats> loadTeamStats(String version) throws IOException {
        String filename = String.format("%s/team_stats_%s.json", modelsPath, version);
        File file = new File(filename);
        
        if (!file.exists()) {
            log.warn("Team stats file not found: {}", filename);
            return new HashMap<>();
        }
        
        Map<String, Object> rawMap = objectMapper.readValue(file, Map.class);
        Map<String, TeamStats> teamStatsMap = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
            TeamStats stats = objectMapper.convertValue(entry.getValue(), TeamStats.class);
            teamStatsMap.put(entry.getKey(), stats);
        }
        
        log.info("Loaded team stats from {}", filename);
        return teamStatsMap;
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> loadMetadata(String version) throws IOException {
        String filename = String.format("%s/metadata_%s.json", modelsPath, version);
        File file = new File(filename);
        
        if (!file.exists()) {
            log.warn("Metadata file not found: {}", filename);
            return new HashMap<>();
        }
        
        Map<String, Object> metadata = objectMapper.readValue(file, Map.class);
        log.info("Loaded metadata from {}", filename);
        return metadata;
    }
    
    public String getModelVersion() {
        return modelVersion;
    }
}
