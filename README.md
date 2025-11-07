# LaLiga Guesser

A Spring Boot application that uses machine learning to predict Spanish LaLiga football match results based on 25 years of historical data.

## Description

This project processes historical match data from football-data.co.uk (2000-2025) and uses a Poisson-based statistical model to predict match outcomes. The system provides:
- Win/Draw/Loss probabilities
- Top 3 most likely scores with percentages
- Feature importance explanations
- REST API for predictions

## Features

- **Poisson Baseline Model**: Uses team attack/defense strength and home advantage
- **Feature Engineering**: Historical performance, form, head-to-head records
- **Time-based Evaluation**: Proper temporal split to avoid data leakage
- **REST API**: Easy-to-use endpoints for predictions and model status
- **Model Persistence**: Save and load trained models

## Prerequisites

- JDK 25 or higher
- Gradle 8.x or higher

## Getting Started

### 1. Build the project

```bash
./gradlew build
```

### 2. Train the model

Train and evaluate the prediction model:

```bash
./gradlew evaluateModel
```

This will:
- Load 25 years of historical LaLiga data
- Train on matches before 2018
- Evaluate on 2018-2019 seasons
- Display accuracy, Brier score, and log-loss metrics
- Save the trained model to the `models/` directory

Example output:
```
Training set: 6,629 matches (before 2018-01-01)
Test set: 591 matches (from 2018-01-01 onwards)
Accuracy: 0.48
Brier Score: 0.21
Log Loss: 1.03
```

### 3. Run the application

Start the REST API server:

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

### 4. Make predictions

Use the prediction API:

```bash
curl -X POST http://localhost:8080/api/predict \
  -H "Content-Type: application/json" \
  -d '{
    "date": "2019-05-20",
    "homeTeam": "Barcelona",
    "awayTeam": "Real Madrid",
    "context": {
      "matchday": 38,
      "competition": "LaLiga"
    }
  }'
```

Response:
```json
{
  "probabilities": {
    "homeWin": 0.57,
    "draw": 0.20,
    "awayWin": 0.23
  },
  "topScores": [
    {"score": "2-1", "prob": 0.089},
    {"score": "1-1", "prob": 0.076},
    {"score": "3-1", "prob": 0.070}
  ],
  "prediction": {
    "outcome": "homeWin",
    "confidence": 0.57
  },
  "explain": [
    {"feature": "home_attack_strength", "impact": 0.776},
    {"feature": "away_attack_strength", "impact": 0.748},
    {"feature": "home_recent_form", "impact": 0.270}
  ],
  "modelVersion": "v0.1"
}
```

## API Endpoints

### Prediction

**POST** `/api/predict`

Predict match outcome and scores.

Request body:
```json
{
  "date": "2019-05-20",
  "homeTeam": "Barcelona",
  "awayTeam": "Real Madrid",
  "context": {
    "matchday": 38,
    "competition": "LaLiga"
  }
}
```

### Model Status

**GET** `/api/predict/status`

Check if the model is loaded and ready.

### Reload Model

**POST** `/api/predict/reload`

Reload the model from disk.

## API Documentation

Once the application is running, access the Swagger UI documentation at:

```
http://localhost:8080/swagger-ui.html
```

## Configuration

The application settings can be configured in `src/main/resources/application.properties`:

```properties
# Predictor configuration
predictor.csvPath=laliga.csv
predictor.modelsPath=models
predictor.modelVersion=v0.1
predictor.trainTestSplitYear=2018
predictor.windowSize=10
predictor.minMatchesForFeatures=5
```

## Model Performance

Current model performance (2018-2019 test set):

| Metric | Value |
|--------|-------|
| Accuracy | 48.05% |
| Brier Score | 0.205 |
| Log Loss | 1.031 |

**Confusion Matrix:**
```
           Actual
         H    D    A
Pred H  191   88   72
     D   16   12   10
     A   63   58   81
```

The model performs better than random chance (33%) and provides calibrated probability estimates.

## Development

### Run tests

```bash
./gradlew test
```

### Train model only

```bash
./gradlew trainModel
```

### Build Docker image

```bash
docker build -t laliga-guesser .
docker run -p 8080:8080 laliga-guesser
```

## Technical Details

### Architecture

- **Data Loading**: CSV parser with date normalization and team name mapping
- **Feature Engineering**: Rolling windows for recent form, head-to-head statistics
- **Model**: Poisson distribution for goal prediction with team strength parameters
- **Evaluation**: Time-based split, Brier score, log-loss, accuracy metrics

### Algorithm

The prediction model uses:
1. **Team Strength**: Attack and defense ratings based on historical performance
2. **Home Advantage**: Statistical advantage for home teams
3. **Recent Form**: Performance in last N games
4. **Head-to-Head**: Historical results between specific teams

Expected goals are calculated as:
```
λ_home = home_attack * away_defense * home_advantage * league_avg
λ_away = away_attack * home_defense * league_avg
```

Then Poisson distributions generate score probabilities.

## License

This project is for educational and personal use.

## Data Source

Historical data from [football-data.co.uk](http://www.football-data.co.uk/)

### Key to CSV Data

- **Div** = League Division
- **Date** = Match Date (dd/mm/yy)
- **HomeTeam** = Home Team
- **AwayTeam** = Away Team
- **FTHG** = Full Time Home Goals
- **FTAG** = Full Time Away Goals
- **FTR** = Full Time Result (H=Home Win, D=Draw, A=Away Win)

For full data dictionary, see the [football-data.co.uk notes](http://www.football-data.co.uk/notes.txt).