package com.inazense.laliga_analyzer.predictor;

import com.inazense.laliga_analyzer.predictor.service.TrainerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

import java.util.Map;

@SpringBootApplication
@ComponentScan(basePackages = "com.inazense.laliga_analyzer")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.runner", havingValue = "evaluation")
public class EvaluationRunner implements CommandLineRunner {
    
    private final TrainerService trainerService;
    
    public static void main(String[] args) {
        System.setProperty("app.runner", "evaluation");
        SpringApplication.run(EvaluationRunner.class, args);
    }
    
    @Override
    public void run(String... args) throws Exception {
        log.info("===== Starting Model Evaluation =====");
        
        Map<String, Object> metrics = trainerService.trainAndEvaluate();
        
        log.info("===== Evaluation Complete =====");
        log.info("\n========================================");
        log.info("EVALUATION METRICS");
        log.info("========================================");
        log.info("Accuracy: {}", metrics.get("accuracy"));
        log.info("Brier Score: {}", metrics.get("brierScore"));
        log.info("Log Loss: {}", metrics.get("logLoss"));
        log.info("Total Predictions: {}", metrics.get("totalPredictions"));
        log.info("Correct Predictions: {}", metrics.get("correctPredictions"));
        log.info("\nConfusion Matrix:");
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> confusionMatrix = (Map<String, Integer>) metrics.get("confusionMatrix");
        log.info("             Actual");
        log.info("           H    D    A");
        log.info("Pred  H   {:3}  {:3}  {:3}", confusionMatrix.get("HH"), confusionMatrix.get("HD"), confusionMatrix.get("HA"));
        log.info("      D   {:3}  {:3}  {:3}", confusionMatrix.get("DH"), confusionMatrix.get("DD"), confusionMatrix.get("DA"));
        log.info("      A   {:3}  {:3}  {:3}", confusionMatrix.get("AH"), confusionMatrix.get("AD"), confusionMatrix.get("AA"));
        log.info("========================================");
        
        System.exit(0);
    }
}
