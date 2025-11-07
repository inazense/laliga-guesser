package com.inazense.laliga_analyzer.predictor.controller;

import com.inazense.laliga_analyzer.commons.constants.Endpoints;
import com.inazense.laliga_analyzer.commons.dto.ApiResponse;
import com.inazense.laliga_analyzer.commons.service.ResponseService;
import com.inazense.laliga_analyzer.predictor.dto.PredictionRequest;
import com.inazense.laliga_analyzer.predictor.model.PredictionResult;
import com.inazense.laliga_analyzer.predictor.service.PredictorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.inazense.laliga_analyzer.commons.enums.AppMessages.*;

@RestController
@RequestMapping(Endpoints.PREDICTOR_REQUEST_MAPPING)
@Tag(name = Endpoints.PREDICTOR_TAG_NAME, description = Endpoints.PREDICTOR_TAG_DESC)
@RequiredArgsConstructor
@Slf4j
public class PredictController {
    
    private final PredictorService predictorService;
    private final ResponseService responseService;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Operation(summary = Endpoints.PREDICTOR_ENDPOINT_PREDICT_SUMMARY)
    @PostMapping(Endpoints.PREDICTOR_ENDPOINT_PREDICT_PATH)
    public ResponseEntity<ApiResponse> predict(@RequestBody PredictionRequest request) {
        try {
            if (!predictorService.isModelLoaded()) {
                return ResponseEntity.status(503)
                        .body(responseService.createResponse(
								PREDICTOR_PREDICT_FAIL_MODEL, 
                                null
                        ));
            }
            
            LocalDate matchDate;
            try {
                matchDate = LocalDate.parse(request.getDate(), DATE_FORMATTER);
            } catch (Exception e) {
                return ResponseEntity.badRequest()
                        .body(responseService.createResponse(
								PREDICTOR_PREDICT_FAIL_DATE, 
                                null
                        ));
            }
            
            PredictionResult result = predictorService.predict(
                    request.getHomeTeam(), 
                    request.getAwayTeam(), 
                    matchDate
            );
            
            return ResponseEntity.ok(
                    responseService.createResponse(PREDICTOR_PREDICT_OK, result)
            );
            
        } catch (Exception e) {
            log.error("Prediction failed", e);
            return ResponseEntity.internalServerError()
                    .body(responseService.createResponse(
                            DOWNLOADER_DOWNLOAD_FAIL, 
                            "Prediction failed: " + e.getMessage()
                    ));
        }
    }
    
    @Operation(summary = "Check model status")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse> status() {
        boolean loaded = predictorService.isModelLoaded();
        return ResponseEntity.ok(
                responseService.createResponse(
                        DOWNLOADER_DOWNLOAD_OK, 
                        "Model loaded: " + loaded
                )
        );
    }
    
    @Operation(summary = "Reload model")
    @PostMapping("/reload")
    public ResponseEntity<ApiResponse> reload() {
        try {
            predictorService.loadModel();
            return ResponseEntity.ok(
                    responseService.createResponse(DOWNLOADER_DOWNLOAD_OK, "Model reloaded successfully")
            );
        } catch (Exception e) {
            log.error("Model reload failed", e);
            return ResponseEntity.internalServerError()
                    .body(responseService.createResponse(
                            DOWNLOADER_DOWNLOAD_FAIL, 
                            "Model reload failed: " + e.getMessage()
                    ));
        }
    }
}
