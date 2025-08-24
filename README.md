
# LaLiga Predictor

A Python application that predicts LaLiga match results using machine learning and historical data.

## Features

- **Automatic loading** of CSV data from multiple seasons
- **SQLite database** for efficient storage
- **Statistics generation** for each team
- **Random Forest model** for predictions
- **Probability predictions** for win/draw/loss

## Installation

1. Install dependencies using a virtual environment:
```bash
python3 -m venv .venv

# Activate it
source .venv/bin/activate

# Install dependencies
pip install -r requirements.txt
```

## Usage

### Full execution
To run the complete pipeline (loading, processing, training, and examples):

```bash
python laliga_predictor.py
```

### Specific prediction
To predict a specific match:

```bash
python predict_match.py "Home Team" "Away Team"
```

Example:
```bash
python predict_match.py "Valencia" "Barcelona"
```

### Programmatic usage
```python
from laliga_predictor import LaLigaPredictor

# Create predictor
predictor = LaLigaPredictor()

# Run complete pipeline
predictor.run_complete_pipeline()

# Make prediction
result = predictor.predict_match("Valencia", "Barcelona")
print(result)
```

## Data structure

CSV files must have the following columns:
- `Date`: Match date (dd/mm/yy)
- `HomeTeam`: Home team
- `AwayTeam`: Away team
- `FTHG`: Home team goals (full time)
- `FTAG`: Away team goals (full time)
- `FTR`: Final result (H=Home win, D=Draw, A=Away win)
- `HTHG`: Home team goals (half time)
- `HTAG`: Away team goals (half time)
- `HTR`: Half time result
- `HS`: Home team shots
- `AS`: Away team shots

## Model features

The model uses the following statistics for each team:
- Average goals scored and conceded
- Overall and home/away win rate
- Average shots
- Recent form (last 5 matches)
- Statistical differences between teams

## Output

The model returns probabilities for:
- **Home Win**: Probability that the home team wins
- **Draw**: Probability of a draw
- **Away Win**: Probability that the away team wins

## Generated files

- `laliga_data.db`: SQLite database with all matches
- Trained model (stored in memory during the session)

## Available teams

The application automatically detects all available teams in the CSV files.


## Technical notes

- The model uses Random Forest with 100 trees
- The last 10 matches are used to calculate statistics
- Typical model accuracy is 50-60%
- Data is split into 80% training and 20% validation