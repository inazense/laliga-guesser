package com.inazense.laliga_analyzer.predictor.util;

import java.util.HashMap;
import java.util.Map;

public class TeamNormalizer {
    
    private static final Map<String, String> TEAM_NAME_MAPPINGS = new HashMap<>();
    
    static {
        // Common variations of team names
        TEAM_NAME_MAPPINGS.put("Ath Bilbao", "Athletic Bilbao");
        TEAM_NAME_MAPPINGS.put("Ath Madrid", "Atletico Madrid");
        TEAM_NAME_MAPPINGS.put("Atletico", "Atletico Madrid");
        TEAM_NAME_MAPPINGS.put("Atl Madrid", "Atletico Madrid");
        TEAM_NAME_MAPPINGS.put("Espanol", "Espanyol");
        TEAM_NAME_MAPPINGS.put("La Coruna", "Deportivo La Coruna");
        TEAM_NAME_MAPPINGS.put("Deportivo", "Deportivo La Coruna");
        TEAM_NAME_MAPPINGS.put("Vallecano", "Rayo Vallecano");
        TEAM_NAME_MAPPINGS.put("Sociedad", "Real Sociedad");
        TEAM_NAME_MAPPINGS.put("Sp Gijon", "Sporting Gijon");
        TEAM_NAME_MAPPINGS.put("Betis", "Real Betis");
        TEAM_NAME_MAPPINGS.put("Santander", "Racing Santander");
        TEAM_NAME_MAPPINGS.put("Racing", "Racing Santander");
        TEAM_NAME_MAPPINGS.put("Sevilla FC", "Sevilla");
        TEAM_NAME_MAPPINGS.put("Granada CF", "Granada");
        TEAM_NAME_MAPPINGS.put("Cadiz CF", "Cadiz");
        TEAM_NAME_MAPPINGS.put("Leganes", "CD Leganes");
        TEAM_NAME_MAPPINGS.put("Almeria", "UD Almeria");
        TEAM_NAME_MAPPINGS.put("Alaves", "Deportivo Alaves");
    }
    
    public static String normalize(String teamName) {
        if (teamName == null) {
            return null;
        }
        
        // Trim whitespace
        String normalized = teamName.trim();
        
        // Check if there's a mapping
        if (TEAM_NAME_MAPPINGS.containsKey(normalized)) {
            normalized = TEAM_NAME_MAPPINGS.get(normalized);
        }
        
        return normalized;
    }
}
