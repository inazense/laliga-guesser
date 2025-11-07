package com.inazense.laliga_analyzer.predictor.service;

import com.inazense.laliga_analyzer.predictor.model.Match;
import com.inazense.laliga_analyzer.predictor.util.TeamNormalizer;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CsvDataLoader {
    
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("dd/MM/yy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd")
    };
    
    public List<Match> loadMatches(String csvPath) throws IOException, CsvException {
        List<Match> matches = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new FileReader(csvPath))) {
            List<String[]> rows = reader.readAll();
            
            if (rows.isEmpty()) {
                log.warn("CSV file is empty: {}", csvPath);
                return matches;
            }
            
            // Parse header
            String[] header = rows.get(0);
            Map<String, Integer> columnIndex = buildColumnIndex(header);
            
            // Parse data rows
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                
                try {
                    Match match = parseMatch(row, columnIndex);
                    if (match != null) {
                        matches.add(match);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse row {}: {}", i, e.getMessage());
                }
            }
        }
        
        log.info("Loaded {} matches from {}", matches.size(), csvPath);
        return matches;
    }
    
    private Map<String, Integer> buildColumnIndex(String[] header) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            index.put(header[i].trim(), i);
        }
        return index;
    }
    
    private Match parseMatch(String[] row, Map<String, Integer> columnIndex) {
        // Required fields
        String homeTeam = getStringValue(row, columnIndex, "HomeTeam");
        String awayTeam = getStringValue(row, columnIndex, "AwayTeam");
        String dateStr = getStringValue(row, columnIndex, "Date");
        
        if (homeTeam == null || awayTeam == null || dateStr == null) {
            return null;
        }
        
        LocalDate date = parseDate(dateStr);
        if (date == null) {
            return null;
        }
        
        Match.MatchBuilder builder = Match.builder()
                .division(getStringValue(row, columnIndex, "Div"))
                .date(date)
                .homeTeam(TeamNormalizer.normalize(homeTeam))
                .awayTeam(TeamNormalizer.normalize(awayTeam))
                .fullTimeHomeGoals(getIntValue(row, columnIndex, "FTHG"))
                .fullTimeAwayGoals(getIntValue(row, columnIndex, "FTAG"))
                .fullTimeResult(getStringValue(row, columnIndex, "FTR"))
                .halfTimeHomeGoals(getIntValue(row, columnIndex, "HTHG"))
                .halfTimeAwayGoals(getIntValue(row, columnIndex, "HTAG"))
                .halfTimeResult(getStringValue(row, columnIndex, "HTR"));
        
        // Optional statistics
        builder.homeShots(getIntValue(row, columnIndex, "HS"))
                .awayShots(getIntValue(row, columnIndex, "AS"))
                .homeShotsOnTarget(getIntValue(row, columnIndex, "HST"))
                .awayShotsOnTarget(getIntValue(row, columnIndex, "AST"))
                .homeCorners(getIntValue(row, columnIndex, "HC"))
                .awayCorners(getIntValue(row, columnIndex, "AC"))
                .homeFouls(getIntValue(row, columnIndex, "HF"))
                .awayFouls(getIntValue(row, columnIndex, "AF"))
                .homeYellowCards(getIntValue(row, columnIndex, "HY"))
                .awayYellowCards(getIntValue(row, columnIndex, "AY"))
                .homeRedCards(getIntValue(row, columnIndex, "HR"))
                .awayRedCards(getIntValue(row, columnIndex, "AR"));
        
        // Optional betting odds
        builder.avgHomeOdds(getDoubleValue(row, columnIndex, "AvgH"))
                .avgDrawOdds(getDoubleValue(row, columnIndex, "AvgD"))
                .avgAwayOdds(getDoubleValue(row, columnIndex, "AvgA"));
        
        return builder.build();
    }
    
    private LocalDate parseDate(String dateStr) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate date = LocalDate.parse(dateStr, formatter);
                // Handle 2-digit years
                if (date.getYear() < 100) {
                    date = date.plusYears(date.getYear() < 50 ? 2000 : 1900);
                }
                return date;
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }
        return null;
    }
    
    private String getStringValue(String[] row, Map<String, Integer> index, String columnName) {
        Integer colIndex = index.get(columnName);
        if (colIndex == null || colIndex >= row.length) {
            return null;
        }
        String value = row[colIndex].trim();
        return value.isEmpty() ? null : value;
    }
    
    private Integer getIntValue(String[] row, Map<String, Integer> index, String columnName) {
        String value = getStringValue(row, index, columnName);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private Double getDoubleValue(String[] row, Map<String, Integer> index, String columnName) {
        String value = getStringValue(row, index, columnName);
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
