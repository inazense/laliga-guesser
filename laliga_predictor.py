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
        """Inicializa el predictor final de LaLiga"""
        self.db_path = db_path
        self.model = None
        self.label_encoder = LabelEncoder()
        self.team_quality_scores = {}
        
    def load_csv_files(self):
        """Carga todos los archivos CSV de la carpeta historic"""
        print("Cargando archivos CSV...")
        all_data = []
        
        csv_files = [f for f in os.listdir('historic') if f.endswith('.csv')]
        
        for file in csv_files:
            file_path = os.path.join('historic', file)
            try:
                df = pd.read_csv(file_path)
                season = file.replace('season-', '').replace('.csv', '')
                df['Season'] = season
                all_data.append(df)
                print(f"Cargado: {file} - {len(df)} partidos")
            except Exception as e:
                print(f"Error cargando {file}: {e}")
        
        if all_data:
            combined_df = pd.concat(all_data, ignore_index=True)
            print(f"Total de partidos cargados: {len(combined_df)}")
            return combined_df
        else:
            raise ValueError("No se encontraron archivos CSV válidos")
    
    def create_database(self, df):
        """Crea la base de datos SQLite y almacena los datos"""
        print("Creando base de datos SQLite final...")
        
        conn = sqlite3.connect(self.db_path)
        
        # Crear tabla de partidos
        df.to_sql('matches', conn, if_exists='replace', index=False)
        
        # Crear índices para mejorar el rendimiento
        conn.execute('CREATE INDEX IF NOT EXISTS idx_home_team ON matches(HomeTeam)')
        conn.execute('CREATE INDEX IF NOT EXISTS idx_away_team ON matches(AwayTeam)')
        conn.execute('CREATE INDEX IF NOT EXISTS idx_date ON matches(Date)')
        
        conn.close()
        print(f"Base de datos final creada: {self.db_path}")
    
    def calculate_team_quality_scores(self, df):
        """Calcula puntuaciones de calidad histórica para cada equipo"""
        print("Calculando puntuaciones de calidad de equipos...")
        
        team_stats = {}
        
        for team in df['HomeTeam'].unique():
            # Obtener todos los partidos del equipo
            team_matches = df[(df['HomeTeam'] == team) | (df['AwayTeam'] == team)]
            
            if len(team_matches) == 0:
                continue
            
            # Calcular estadísticas
            home_matches = team_matches[team_matches['HomeTeam'] == team]
            away_matches = team_matches[team_matches['AwayTeam'] == team]
            
            # Estadísticas como local
            home_wins = len(home_matches[home_matches['FTR'] == 'H'])
            home_draws = len(home_matches[home_matches['FTR'] == 'D'])
            home_goals_scored = home_matches['FTHG'].sum()
            home_goals_conceded = home_matches['FTAG'].sum()
            
            # Estadísticas como visitante
            away_wins = len(away_matches[away_matches['FTR'] == 'A'])
            away_draws = len(away_matches[away_matches['FTR'] == 'D'])
            away_goals_scored = away_matches['FTAG'].sum()
            away_goals_conceded = away_matches['FTHG'].sum()
            
            # Calcular puntuación de calidad
            total_matches = len(team_matches)
            total_wins = home_wins + away_wins
            total_draws = home_draws + away_draws
            total_goals_scored = home_goals_scored + away_goals_scored
            total_goals_conceded = home_goals_conceded + away_goals_conceded
            
            # Puntuación basada en victorias, empates y diferencia de goles
            win_rate = total_wins / total_matches if total_matches > 0 else 0
            draw_rate = total_draws / total_matches if total_matches > 0 else 0
            goal_diff_per_match = (total_goals_scored - total_goals_conceded) / total_matches if total_matches > 0 else 0
            
            # Puntuación de calidad (0-100)
            quality_score = (
                win_rate * 50 +  # 50% peso a victorias
                draw_rate * 25 +  # 25% peso a empates
                max(0, goal_diff_per_match + 0.5) * 25  # 25% peso a diferencia de goles
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
        
        # Normalizar puntuaciones (0-100)
        if team_stats:
            max_score = max(stats['quality_score'] for stats in team_stats.values())
            min_score = min(stats['quality_score'] for stats in team_stats.values())
            
            for team in team_stats:
                if max_score > min_score:
                    team_stats[team]['quality_score'] = (
                        (team_stats[team]['quality_score'] - min_score) / (max_score - min_score) * 100
                    )
        
        self.team_quality_scores = team_stats
        print(f"Puntuaciones de calidad calculadas para {len(team_stats)} equipos")
        
        # Mostrar top 10 equipos
        top_teams = sorted(team_stats.items(), key=lambda x: x[1]['quality_score'], reverse=True)[:10]
        print("\nTop 10 equipos por calidad histórica:")
        for i, (team, stats) in enumerate(top_teams, 1):
            print(f"{i:2}. {team:20} - Puntuación: {stats['quality_score']:.1f}")
    
    def get_team_statistics_simple(self, team_name, df, current_date, lookback_matches=10):
        """Calcula estadísticas simples para un equipo"""
        
        # Obtener partidos del equipo antes de la fecha actual
        team_matches = df[
            ((df['HomeTeam'] == team_name) | (df['AwayTeam'] == team_name)) &
            (df['Date'] < current_date)
        ].sort_values('Date', ascending=False).head(lookback_matches)
        
        if len(team_matches) == 0:
            return self._get_default_stats()
        
        # Calcular estadísticas
        home_matches = team_matches[team_matches['HomeTeam'] == team_name]
        away_matches = team_matches[team_matches['AwayTeam'] == team_name]
        
        stats = {}
        
        # Estadísticas como local
        if len(home_matches) > 0:
            stats['home_goals_scored'] = home_matches['FTHG'].mean()
            stats['home_goals_conceded'] = home_matches['FTAG'].mean()
            stats['home_win_rate'] = (home_matches['FTR'] == 'H').mean()
        else:
            stats.update(self._get_default_home_stats())
        
        # Estadísticas como visitante
        if len(away_matches) > 0:
            stats['away_goals_scored'] = away_matches['FTAG'].mean()
            stats['away_goals_conceded'] = away_matches['FTHG'].mean()
            stats['away_win_rate'] = (away_matches['FTR'] == 'A').mean()
        else:
            stats.update(self._get_default_away_stats())
        
        # Forma reciente (últimos 5 partidos)
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
        
        # Agregar puntuación de calidad histórica
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
        """Prepara características simples para el modelo"""
        print("Preparando características simples para el modelo...")
        
        # Calcular puntuaciones de calidad primero
        self.calculate_team_quality_scores(df)
        
        features = []
        labels = []
        
        # Procesar una muestra para entrenamiento
        sample_df = df.sample(min(800, len(df)), random_state=42)
        
        processed_count = 0
        for idx, row in sample_df.iterrows():
            try:
                # Obtener estadísticas de ambos equipos
                home_stats = self.get_team_statistics_simple(row['HomeTeam'], df, row['Date'])
                away_stats = self.get_team_statistics_simple(row['AwayTeam'], df, row['Date'])
                
                # Crear vector de características simple
                feature_vector = [
                    # Estadísticas del equipo local
                    home_stats['home_goals_scored'],
                    home_stats['home_goals_conceded'],
                    home_stats['home_win_rate'],
                    home_stats['recent_form'],
                    home_stats['historical_quality'],
                    
                    # Estadísticas del equipo visitante
                    away_stats['away_goals_scored'],
                    away_stats['away_goals_conceded'],
                    away_stats['away_win_rate'],
                    away_stats['recent_form'],
                    away_stats['historical_quality'],
                    
                    # Diferencias clave
                    home_stats['home_goals_scored'] - away_stats['away_goals_conceded'],
                    away_stats['away_goals_scored'] - home_stats['home_goals_conceded'],
                    home_stats['home_win_rate'] - away_stats['away_win_rate'],
                    home_stats['historical_quality'] - away_stats['historical_quality'],
                    
                    # Factores combinados
                    (home_stats['home_win_rate'] + home_stats['recent_form'] + home_stats['historical_quality']) / 3,
                    (away_stats['away_win_rate'] + away_stats['recent_form'] + away_stats['historical_quality']) / 3,
                    
                    # Ventaja de local (ajustada según calidad del equipo)
                    min(0.25, 0.15 + (home_stats['historical_quality'] - away_stats['historical_quality']) * 0.1),
                    
                    # Factor de forma reciente
                    home_stats['recent_form'] - away_stats['recent_form']
                ]
                
                features.append(feature_vector)
                labels.append(row['FTR'])
                processed_count += 1
                
            except Exception as e:
                continue
        
        # Convertir a arrays de numpy
        X = np.array(features)
        y = np.array(labels)
        
        # Verificar que tenemos suficientes datos
        if len(X) < 50:
            print(f"Error: No hay suficientes datos para entrenar el modelo. Procesados: {processed_count}")
            return None, None
        
        # Codificar las etiquetas
        y_encoded = self.label_encoder.fit_transform(y)
        
        print(f"Características simples preparadas: {X.shape[0]} muestras, {X.shape[1]} características")
        return X, y_encoded
    
    def train_model_simple(self, X, y):
        """Entrena el modelo simple"""
        print("Entrenando modelo simple...")
        
        # Dividir datos en entrenamiento y validación
        X_train, X_val, y_train, y_val = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)
        
        # Crear y entrenar modelo Random Forest
        self.model = RandomForestClassifier(
            n_estimators=100,
            max_depth=6,
            min_samples_split=10,
            min_samples_leaf=5,
            random_state=42,
            n_jobs=-1
        )
        
        self.model.fit(X_train, y_train)
        
        # Evaluar el modelo
        y_pred = self.model.predict(X_val)
        accuracy = accuracy_score(y_val, y_pred)
        
        print(f"Precisión del modelo simple: {accuracy:.3f}")
        
        # Solo mostrar reporte si hay múltiples clases
        unique_classes = len(np.unique(y_val))
        if unique_classes > 1:
            print("\nReporte de clasificación:")
            print(classification_report(y_val, y_pred, target_names=self.label_encoder.classes_))
        
        return accuracy
    
    def predict_match_simple(self, home_team, away_team, df):
        """Predice las probabilidades de un partido específico con el modelo simple"""
        if self.model is None:
            raise ValueError("El modelo no ha sido entrenado. Ejecuta train_model_simple() primero.")
        
        print(f"Prediciendo con modelo simple: {home_team} vs {away_team}")
        
        # Obtener la fecha más reciente para calcular estadísticas
        latest_date = df['Date'].max()
        
        # Obtener estadísticas de ambos equipos
        home_stats = self.get_team_statistics_simple(home_team, df, latest_date)
        away_stats = self.get_team_statistics_simple(away_team, df, latest_date)
        
        # Crear vector de características
        feature_vector = [
            # Estadísticas del equipo local
            home_stats['home_goals_scored'],
            home_stats['home_goals_conceded'],
            home_stats['home_win_rate'],
            home_stats['recent_form'],
            home_stats['historical_quality'],
            
            # Estadísticas del equipo visitante
            away_stats['away_goals_scored'],
            away_stats['away_goals_conceded'],
            away_stats['away_win_rate'],
            away_stats['recent_form'],
            away_stats['historical_quality'],
            
            # Diferencias clave
            home_stats['home_goals_scored'] - away_stats['away_goals_conceded'],
            away_stats['away_goals_scored'] - home_stats['home_goals_conceded'],
            home_stats['home_win_rate'] - away_stats['away_win_rate'],
            home_stats['historical_quality'] - away_stats['historical_quality'],
            
            # Factores combinados
            (home_stats['home_win_rate'] + home_stats['recent_form'] + home_stats['historical_quality']) / 3,
            (away_stats['away_win_rate'] + away_stats['recent_form'] + away_stats['historical_quality']) / 3,
            
            # Ventaja de local (ajustada según calidad del equipo)
            min(0.25, 0.15 + (home_stats['historical_quality'] - away_stats['historical_quality']) * 0.1),
            
            # Factor de forma reciente
            home_stats['recent_form'] - away_stats['recent_form']
        ]
        
        # Hacer predicción
        X = np.array([feature_vector])
        probabilities = self.model.predict_proba(X)[0]
        
        # Crear diccionario de resultados
        results = {}
        for i, class_name in enumerate(self.label_encoder.classes_):
            if class_name == 'H':
                results['Victoria Local'] = f"{probabilities[i]:.1%}"
            elif class_name == 'D':
                results['Empate'] = f"{probabilities[i]:.1%}"
            elif class_name == 'A':
                results['Victoria Visitante'] = f"{probabilities[i]:.1%}"
        
        return results
    
    def run_complete_pipeline_simple(self):
        """Ejecuta el pipeline completo simple"""
        print("=== LA LIGA PREDICTOR FINAL ===")
        print("Iniciando pipeline completo simple...\n")
        
        # 1. Cargar datos CSV
        df = self.load_csv_files()
        
        # 2. Crear base de datos SQLite
        self.create_database(df)
        
        # 3. Preparar características simples
        X, y = self.prepare_features_simple(df)
        
        if X is None or y is None:
            print("Error: No se pudieron preparar las características")
            return False
        
        # 4. Entrenar modelo simple
        accuracy = self.train_model_simple(X, y)
        
        print(f"\n=== MODELO FINAL ENTRENADO CON ÉXITO ===")
        print(f"Precisión: {accuracy:.3f}")
        
        return True
    
    def get_available_teams(self, df):
        """Obtiene la lista de equipos disponibles"""
        return df['HomeTeam'].unique().tolist()

def main():
    """Función principal para ejecutar el predictor final"""
    predictor = LaLigaPredictorFinal()
    
    try:
        # Ejecutar pipeline completo simple
        success = predictor.run_complete_pipeline_simple()
        
        if not success:
            print("Error en el pipeline")
            return
        
        # Cargar datos para predicciones
        df = predictor.load_csv_files()
        
        # Ejemplo de predicciones
        print("\n=== EJEMPLOS DE PREDICCIONES FINALES ===")
        
        # Obtener equipos disponibles
        teams = predictor.get_available_teams(df)
        print(f"Equipos disponibles: {teams}...")
        
        # Ejemplos de predicciones
        print("\n=== USO ===")
        print("Para hacer una predicción personalizada:")
        print("predictor = LaLigaPredictorFinal()")
        print("predictor.run_complete_pipeline_simple()")
        print("df = predictor.load_csv_files()")
        print("result = predictor.predict_match_simple('Equipo Local', 'Equipo Visitante', df)")
        
    except Exception as e:
        print(f"Error en el pipeline: {e}")

if __name__ == "__main__":
    main()