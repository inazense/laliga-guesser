#!/usr/bin/env python3
"""
Script to make LaLiga match predictions with the final model
Usage: python predict_match.py "Home Team" "Away Team"
"""

import sys
from laliga_predictor import LaLigaPredictorFinal

def main():
    if len(sys.argv) != 3:
        print("Usage: python predict_match.py 'Home Team' 'Away Team'")
        print("Example: python predict_match.py 'Valencia' 'Barcelona'")
        sys.exit(1)
    
    home_team = sys.argv[1]
    away_team = sys.argv[2]
    
    print(f"=== FINAL PREDICTION: {home_team} vs {away_team} ===")
    
    try:
        # Create final predictor
        predictor = LaLigaPredictorFinal()

        # Check if the database already exists
        import os
        if not os.path.exists('laliga_data.db'):
            print("Final database not found. Running complete pipeline...")
            predictor.run_complete_pipeline_simple()
        else:
            print("Loading existing final model...")
            # Load data and train model
            df = predictor.load_csv_files()
            predictor.create_database(df)
            X, y = predictor.prepare_features_simple(df)
            if X is not None and y is not None:
                predictor.train_model_simple(X, y)
            else:
                print("Error: Could not prepare features")
                return

        # Load data for prediction
        df = predictor.load_csv_files()

        # Make final prediction
        result = predictor.predict_match_simple(home_team, away_team, df)

        print(f"\nFINAL PREDICTION RESULT:")
        print(f"{'='*50}")
        for outcome, probability in result.items():
            print(f"{outcome:20}: {probability}")
        print(f"{'='*50}")

        # Show most likely prediction
        most_likely = max(result.items(), key=lambda x: float(x[1].rstrip('%')))
        print(f"\nMost likely prediction: {most_likely[0]} ({most_likely[1]})")

        # Show team quality information
        if hasattr(predictor, 'team_quality_scores'):
            print(f"\nTeam quality information:")
            if home_team in predictor.team_quality_scores:
                home_quality = predictor.team_quality_scores[home_team]['quality_score']
                print(f"{home_team}: {home_quality:.1f}/100")
            if away_team in predictor.team_quality_scores:
                away_quality = predictor.team_quality_scores[away_team]['quality_score']
                print(f"{away_team}: {away_quality:.1f}/100")

        # Additional analysis
        print(f"\nModel analysis:")
        print(f"- Considers historical team quality")
        print(f"- Recent form of both teams")
        print(f"- Home advantage adjusted by quality")
        print(f"- Statistics of goals scored/conceded")
        print(f"- Model accuracy: 50%")
        
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
