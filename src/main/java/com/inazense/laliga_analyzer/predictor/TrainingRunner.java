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
@ConditionalOnProperty(name = "app.runner", havingValue = "training")
public class TrainingRunner implements CommandLineRunner {
    
    private final TrainerService trainerService;
    
    public static void main(String[] args) {
        System.setProperty("app.runner", "training");
        SpringApplication.run(TrainingRunner.class, args);
    }
    
    @Override
    public void run(String... args) throws Exception {
        log.info("===== Starting Model Training =====");
        
        Map<String, Object> metrics = trainerService.trainAndEvaluate();
        
        log.info("===== Training Complete =====");
        log.info("Metrics:");
        for (Map.Entry<String, Object> entry : metrics.entrySet()) {
            log.info("  {}: {}", entry.getKey(), entry.getValue());
        }
        
        System.exit(0);
    }
}
