import pandas as pd
import numpy as np
import sqlite3
import os
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import classification_report, accuracy_score
import warnings
warnings.filterwarnings('ignore')

class LaLigaPredictorFinal:
    def __init__(self, db_path='laliga_data.db'):
        """Initializes the final LaLiga predictor"""
        self.db_path = db_path
        self.model = None
        self.label_encoder = LabelEncoder()
        self.team_quality_scores = {}
        
    def load_csv_files(self):
        """Loads all CSV files from the historic folder"""
        print("Loading CSV files...")
        all_data = []

        csv_files = [f for f in os.listdir('historic') if f.endswith('.csv')]

        for file in csv_files:
            file_path = os.path.join('historic', file)
            try:
                df = pd.read_csv(file_path)
                season = file.replace('season-', '').replace('.csv', '')
                df['Season'] = season
                all_data.append(df)
                print(f"Loaded: {file} - {len(df)} matches")
            except Exception as e:
                print(f"Error loading {file}: {e}")

        if all_data:
            combined_df = pd.concat(all_data, ignore_index=True)
            print(f"Total matches loaded: {len(combined_df)}")
            return combined_df
        else:
            raise ValueError("No valid CSV files found")
    
    def create_database(self, df):
        """Creates the SQLite database and stores the data"""
        print("Creating final SQLite database...")

        conn = sqlite3.connect(self.db_path)

        # Create matches table
        df.to_sql('matches', conn, if_exists='replace', index=False)

        # Create indexes to improve performance
        conn.execute('CREATE INDEX IF NOT EXISTS idx_home_team ON matches(HomeTeam)')
        conn.execute('CREATE INDEX IF NOT EXISTS idx_away_team ON matches(AwayTeam)')
        conn.execute('CREATE INDEX IF NOT EXISTS idx_date ON matches(Date)')

        conn.close()
        print(f"Final database created: {self.db_path}")
    
    def calculate_team_quality_scores(self, df):
        """Calculates historical quality scores for each team"""
        print("Calculating team quality scores...")

        team_stats = {}

        for team in df['HomeTeam'].unique():
            # Get all matches for the team
            team_matches = df[(df['HomeTeam'] == team) | (df['AwayTeam'] == team)]

            if len(team_matches) == 0:
                continue

            # Calculate statistics
            home_matches = team_matches[team_matches['HomeTeam'] == team]
            away_matches = team_matches[team_matches['AwayTeam'] == team]

            # Home statistics
            home_wins = len(home_matches[home_matches['FTR'] == 'H'])
            home_draws = len(home_matches[home_matches['FTR'] == 'D'])
            home_goals_scored = home_matches['FTHG'].sum()
            home_goals_conceded = home_matches['FTAG'].sum()

            # Away statistics
            away_wins = len(away_matches[away_matches['FTR'] == 'A'])
            away_draws = len(away_matches[away_matches['FTR'] == 'D'])
            away_goals_scored = away_matches['FTAG'].sum()
            away_goals_conceded = away_matches['FTHG'].sum()

            # Calculate quality score
            total_matches = len(team_matches)
            total_wins = home_wins + away_wins
            total_draws = home_draws + away_draws
            total_goals_scored = home_goals_scored + away_goals_scored
            total_goals_conceded = home_goals_conceded + away_goals_conceded

            # Score based on wins, draws, and goal difference
            win_rate = total_wins / total_matches if total_matches > 0 else 0
            draw_rate = total_draws / total_matches if total_matches > 0 else 0
            goal_diff_per_match = (total_goals_scored - total_goals_conceded) / total_matches if total_matches > 0 else 0

            # Quality score (0-100)
            quality_score = (
                win_rate * 50 +  # 50% weight to wins
                draw_rate * 25 +  # 25% weight to draws
                max(0, goal_diff_per_match + 0.5) * 25  # 25% weight to goal difference
            )

            team_stats[team] = {
                'quality_score': quality_score,
                'win_rate': win_rate,
                'draw_rate': draw_rate,
                'goal_diff_per_match': goal_diff_per_match,
                'total_matches': total_matches,
                'home_win_rate': home_wins / len(home_matches) if len(home_matches) > 0 else 0,
                'away_win_rate': away_wins / len(away_matches) if len(away_matches) > 0 else 0
            }

        # Normalize scores (0-100)
        if team_stats:
            max_score = max(stats['quality_score'] for stats in team_stats.values())
            min_score = min(stats['quality_score'] for stats in team_stats.values())

            for team in team_stats:
                if max_score > min_score:
                    team_stats[team]['quality_score'] = (
                        (team_stats[team]['quality_score'] - min_score) / (max_score - min_score) * 100
                    )

        self.team_quality_scores = team_stats
        print(f"Quality scores calculated for {len(team_stats)} teams")

        # Show top 10 teams
        top_teams = sorted(team_stats.items(), key=lambda x: x[1]['quality_score'], reverse=True)[:10]
        print("\nTop 10 teams by historical quality:")
        for i, (team, stats) in enumerate(top_teams, 1):
            print(f"{i:2}. {team:20} - Score: {stats['quality_score']:.1f}")
    
    def get_team_statistics_simple(self, team_name, df, current_date, lookback_matches=10):
        """Calculates simple statistics for a team"""

        # Get matches for the team before the current date
        team_matches = df[
            ((df['HomeTeam'] == team_name) | (df['AwayTeam'] == team_name)) &
            (df['Date'] < current_date)
        ].sort_values('Date', ascending=False).head(lookback_matches)

        if len(team_matches) == 0:
            return self._get_default_stats()

        # Calculate statistics
        home_matches = team_matches[team_matches['HomeTeam'] == team_name]
        away_matches = team_matches[team_matches['AwayTeam'] == team_name]

        stats = {}

        # Home statistics
        if len(home_matches) > 0:
            stats['home_goals_scored'] = home_matches['FTHG'].mean()
            stats['home_goals_conceded'] = home_matches['FTAG'].mean()
            stats['home_win_rate'] = (home_matches['FTR'] == 'H').mean()
        else:
            stats.update(self._get_default_home_stats())

        # Away statistics
        if len(away_matches) > 0:
            stats['away_goals_scored'] = away_matches['FTAG'].mean()
            stats['away_goals_conceded'] = away_matches['FTHG'].mean()
            stats['away_win_rate'] = (away_matches['FTR'] == 'A').mean()
        else:
            stats.update(self._get_default_away_stats())

        # Recent form (last 5 matches)
        recent_matches = team_matches.head(5)
        if len(recent_matches) > 0:
            recent_wins = 0
            for _, match in recent_matches.iterrows():
                if match['HomeTeam'] == team_name and match['FTR'] == 'H':
                    recent_wins += 1
                elif match['AwayTeam'] == team_name and match['FTR'] == 'A':
                    recent_wins += 1
            stats['recent_form'] = recent_wins / len(recent_matches)
        else:
            stats['recent_form'] = 0.5

        # Add historical quality score
        if team_name in self.team_quality_scores:
            stats['historical_quality'] = self.team_quality_scores[team_name]['quality_score'] / 100.0
        else:
            stats['historical_quality'] = 0.5

        return stats
    
    def _get_default_stats(self):
        return {
            'home_goals_scored': 1.0, 'home_goals_conceded': 1.0, 'home_win_rate': 0.33,
            'away_goals_scored': 1.0, 'away_goals_conceded': 1.0, 'away_win_rate': 0.33,
            'recent_form': 0.5, 'historical_quality': 0.5
        }
    
    def _get_default_home_stats(self):
        return {
            'home_goals_scored': 1.0, 'home_goals_conceded': 1.0, 'home_win_rate': 0.33
        }
    
    def _get_default_away_stats(self):
        return {
            'away_goals_scored': 1.0, 'away_goals_conceded': 1.0, 'away_win_rate': 0.33
        }
    
    def prepare_features_simple(self, df):
        """Prepares simple features for the model"""
        print("Preparing simple features for the model...")

        # Calculate quality scores first
        self.calculate_team_quality_scores(df)

        features = []
        labels = []

        # Process a sample for training
        sample_df = df.sample(min(800, len(df)), random_state=42)

        processed_count = 0
        for idx, row in sample_df.iterrows():
            try:
                # Get statistics for both teams
                home_stats = self.get_team_statistics_simple(row['HomeTeam'], df, row['Date'])
                away_stats = self.get_team_statistics_simple(row['AwayTeam'], df, row['Date'])

                # Create simple feature vector
                feature_vector = [
                    # Home team statistics
                    home_stats['home_goals_scored'],
                    home_stats['home_goals_conceded'],
                    home_stats['home_win_rate'],
                    home_stats['recent_form'],
                    home_stats['historical_quality'],

                    # Away team statistics
                    away_stats['away_goals_scored'],
                    away_stats['away_goals_conceded'],
                    away_stats['away_win_rate'],
                    away_stats['recent_form'],
                    away_stats['historical_quality'],

                    # Key differences
                    home_stats['home_goals_scored'] - away_stats['away_goals_conceded'],
                    away_stats['away_goals_scored'] - home_stats['home_goals_conceded'],
                    home_stats['home_win_rate'] - away_stats['away_win_rate'],
                    home_stats['historical_quality'] - away_stats['historical_quality'],

                    # Combined factors
                    (home_stats['home_win_rate'] + home_stats['recent_form'] + home_stats['historical_quality']) / 3,
                    (away_stats['away_win_rate'] + away_stats['recent_form'] + away_stats['historical_quality']) / 3,

                    # Home advantage (adjusted by team quality)
                    min(0.25, 0.15 + (home_stats['historical_quality'] - away_stats['historical_quality']) * 0.1),

                    # Recent form factor
                    home_stats['recent_form'] - away_stats['recent_form']
                ]

                features.append(feature_vector)
                labels.append(row['FTR'])
                processed_count += 1

            except Exception as e:
                continue

        # Convert to numpy arrays
        X = np.array(features)
        y = np.array(labels)

        # Check that we have enough data
        if len(X) < 50:
            print(f"Error: Not enough data to train the model. Processed: {processed_count}")
            return None, None

        # Encode labels
        y_encoded = self.label_encoder.fit_transform(y)

        print(f"Simple features prepared: {X.shape[0]} samples, {X.shape[1]} features")
        return X, y_encoded
    
    def train_model_simple(self, X, y):
        """Trains the simple model"""
        print("Training simple model...")

        # Split data into training and validation
        X_train, X_val, y_train, y_val = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

        # Create and train Random Forest model
        self.model = RandomForestClassifier(
            n_estimators=100,
            max_depth=6,
            min_samples_split=10,
            min_samples_leaf=5,
            random_state=42,
            n_jobs=-1
        )

        self.model.fit(X_train, y_train)

        # Evaluate the model
        y_pred = self.model.predict(X_val)
        accuracy = accuracy_score(y_val, y_pred)

        print(f"Simple model accuracy: {accuracy:.3f}")

        # Only show report if there are multiple classes
        unique_classes = len(np.unique(y_val))
        if unique_classes > 1:
            print("\nClassification report:")
            print(classification_report(y_val, y_pred, target_names=self.label_encoder.classes_))

        return accuracy
    
    def predict_match_simple(self, home_team, away_team, df):
        """Predicts the probabilities of a specific match with the simple model"""
        if self.model is None:
            raise ValueError("The model has not been trained. Run train_model_simple() first.")

        print(f"Predicting with simple model: {home_team} vs {away_team}")

        # Get the most recent date to calculate statistics
        latest_date = df['Date'].max()

        # Get statistics for both teams
        home_stats = self.get_team_statistics_simple(home_team, df, latest_date)
        away_stats = self.get_team_statistics_simple(away_team, df, latest_date)

        # Create feature vector
        feature_vector = [
            # Home team statistics
            home_stats['home_goals_scored'],
            home_stats['home_goals_conceded'],
            home_stats['home_win_rate'],
            home_stats['recent_form'],
            home_stats['historical_quality'],
            
            # Away team statistics
            away_stats['away_goals_scored'],
            away_stats['away_goals_conceded'],
            away_stats['away_win_rate'],
            away_stats['recent_form'],
            away_stats['historical_quality'],
            
            # Key differences
            home_stats['home_goals_scored'] - away_stats['away_goals_conceded'],
            away_stats['away_goals_scored'] - home_stats['home_goals_conceded'],
            home_stats['home_win_rate'] - away_stats['away_win_rate'],
            home_stats['historical_quality'] - away_stats['historical_quality'],
            
            # Combined factors
            (home_stats['home_win_rate'] + home_stats['recent_form'] + home_stats['historical_quality']) / 3,
            (away_stats['away_win_rate'] + away_stats['recent_form'] + away_stats['historical_quality']) / 3,
            
            # Home advantage (adjusted by team quality)
            min(0.25, 0.15 + (home_stats['historical_quality'] - away_stats['historical_quality']) * 0.1),
            
            # Recent form factor
            home_stats['recent_form'] - away_stats['recent_form']
        ]
        
        # Make prediction
        X = np.array([feature_vector])
        probabilities = self.model.predict_proba(X)[0]
        
        # Create results dictionary
        results = {}
        for i, class_name in enumerate(self.label_encoder.classes_):
            if class_name == 'H':
                results['Home Win'] = f"{probabilities[i]:.1%}"
            elif class_name == 'D':
                results['Draw'] = f"{probabilities[i]:.1%}"
            elif class_name == 'A':
                results['Away Win'] = f"{probabilities[i]:.1%}"
        
        return results
    
    def run_complete_pipeline_simple(self):
        """Runs the complete simple pipeline"""
        print("=== LA LIGA PREDICTOR FINAL ===")
        print("Starting complete simple pipeline...\n")
        
        # 1. Load CSV data
        df = self.load_csv_files()
        
        # 2. Create SQLite database
        self.create_database(df)
        
        # 3. Prepare simple features
        X, y = self.prepare_features_simple(df)
        
        if X is None or y is None:
            print("Error: Could not prepare features")
            return False
        
        # 4. Train simple model
        accuracy = self.train_model_simple(X, y)
        
        print(f"\n=== FINAL MODEL TRAINED SUCCESSFULLY ===")
        print(f"Accuracy: {accuracy:.3f}")
        
        return True
    
    def get_available_teams(self, df):
        """Gets the list of available teams"""
        return df['HomeTeam'].unique().tolist()

def main():
    """Main function to run the final predictor"""
    predictor = LaLigaPredictorFinal()
    
    try:
        # Run complete simple pipeline
        success = predictor.run_complete_pipeline_simple()
        
        if not success:
            print("Error in pipeline")
            return
        
        # Load data for predictions
        df = predictor.load_csv_files()
        
        # Example predictions
        print("\n=== FINAL PREDICTION EXAMPLES ===")
        
        # Get available teams
        teams = predictor.get_available_teams(df)
        print(f"Available teams: {teams}...")
        
        # Example predictions
        print("\n=== USAGE ===")
        print("To make a custom prediction:")
        print("predictor = LaLigaPredictorFinal()")
        print("predictor.run_complete_pipeline_simple()")
        print("df = predictor.load_csv_files()")
        print("result = predictor.predict_match_simple('Home Team', 'Away Team', df)")
        
    except Exception as e:
        print(f"Error in pipeline: {e}")

if __name__ == "__main__":
    main()