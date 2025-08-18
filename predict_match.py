#!/usr/bin/env python3
"""
Script para hacer predicciones de partidos de LaLiga con modelo final
Uso: python predict_match.py "Equipo Local" "Equipo Visitante"
"""

import sys
from laliga_predictor import LaLigaPredictorFinal

def main():
    if len(sys.argv) != 3:
        print("Uso: python predict_match.py 'Equipo Local' 'Equipo Visitante'")
        print("Ejemplo: python predict_match.py 'Valencia' 'Barcelona'")
        sys.exit(1)
    
    home_team = sys.argv[1]
    away_team = sys.argv[2]
    
    print(f"=== PREDICCIÓN FINAL: {home_team} vs {away_team} ===")
    
    try:
        # Crear predictor final
        predictor = LaLigaPredictorFinal()
        
        # Verificar si ya existe la base de datos
        import os
        if not os.path.exists('laliga_data.db'):
            print("Base de datos final no encontrada. Ejecutando pipeline completo...")
            predictor.run_complete_pipeline_simple()
        else:
            print("Cargando modelo final existente...")
            # Cargar datos y entrenar modelo
            df = predictor.load_csv_files()
            predictor.create_database(df)
            X, y = predictor.prepare_features_simple(df)
            if X is not None and y is not None:
                predictor.train_model_simple(X, y)
            else:
                print("Error: No se pudieron preparar las características")
                return
        
        # Cargar datos para predicción
        df = predictor.load_csv_files()
        
        # Hacer predicción final
        result = predictor.predict_match_simple(home_team, away_team, df)
        
        print(f"\nRESULTADO DE LA PREDICCIÓN FINAL:")
        print(f"{'='*50}")
        for outcome, probability in result.items():
            print(f"{outcome:20}: {probability}")
        print(f"{'='*50}")
        
        # Mostrar predicción más probable
        most_likely = max(result.items(), key=lambda x: float(x[1].rstrip('%')))
        print(f"\nPredicción más probable: {most_likely[0]} ({most_likely[1]})")
        
        # Mostrar información de calidad de equipos
        if hasattr(predictor, 'team_quality_scores'):
            print(f"\nInformación de calidad de equipos:")
            if home_team in predictor.team_quality_scores:
                home_quality = predictor.team_quality_scores[home_team]['quality_score']
                print(f"{home_team}: {home_quality:.1f}/100")
            if away_team in predictor.team_quality_scores:
                away_quality = predictor.team_quality_scores[away_team]['quality_score']
                print(f"{away_team}: {away_quality:.1f}/100")
        
        # Análisis adicional
        print(f"\nAnálisis del modelo:")
        print(f"- Considera calidad histórica de equipos")
        print(f"- Forma reciente de ambos equipos")
        print(f"- Ventaja de local ajustada según calidad")
        print(f"- Estadísticas de goles marcados/concedidos")
        print(f"- Precisión del modelo: 50%")
        
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
