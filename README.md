# LaLiga Predictor

Una aplicación de Python que predice resultados de partidos de LaLiga usando machine learning y datos históricos.

## Características

- **Carga automática** de datos CSV de múltiples temporadas
- **Base de datos SQLite** para almacenamiento eficiente
- **Generación de estadísticas** para cada equipo
- **Modelo Random Forest** para predicciones
- **Predicciones de probabilidades** para victoria/empate/derrota

## Instalación

1. Instala las dependencias usando un entorno virtual:
```bash
python3 -m venv .venv

# Activarlo
source .venv/bin/activate

# Instalar dependencias
pip install -r requirements.txt
```

## Uso

### Ejecución completa
Para ejecutar el pipeline completo (carga, procesamiento, entrenamiento y ejemplos):

```bash
python laliga_predictor.py
```

### Predicción específica
Para predecir un partido específico:

```bash
python predict_match.py "Equipo Local" "Equipo Visitante"
```

Ejemplo:
```bash
python predict_match.py "Valencia" "Barcelona"
```

### Uso programático
```python
from laliga_predictor import LaLigaPredictor

# Crear predictor
predictor = LaLigaPredictor()

# Ejecutar pipeline completo
predictor.run_complete_pipeline()

# Hacer predicción
result = predictor.predict_match("Valencia", "Barcelona")
print(result)
```

## Estructura de datos

Los archivos CSV deben tener las siguientes columnas:
- `Date`: Fecha del partido (dd/mm/yy)
- `HomeTeam`: Equipo local
- `AwayTeam`: Equipo visitante
- `FTHG`: Goles del equipo local (tiempo completo)
- `FTAG`: Goles del equipo visitante (tiempo completo)
- `FTR`: Resultado final (H=Victoria local, D=Empate, A=Victoria visitante)
- `HTHG`: Goles del equipo local (primer tiempo)
- `HTAG`: Goles del equipo visitante (primer tiempo)
- `HTR`: Resultado del primer tiempo
- `HS`: Tiros del equipo local
- `AS`: Tiros del equipo visitante

## Características del modelo

El modelo utiliza las siguientes estadísticas para cada equipo:
- Promedio de goles marcados y concedidos
- Tasa de victoria general y en casa/fuera
- Promedio de tiros
- Forma reciente (últimos 5 partidos)
- Diferencias de estadísticas entre equipos

## Salida

El modelo devuelve las probabilidades para:
- **Victoria Local**: Probabilidad de que gane el equipo local
- **Empate**: Probabilidad de empate
- **Victoria Visitante**: Probabilidad de que gane el equipo visitante

## Archivos generados

- `laliga_data.db`: Base de datos SQLite con todos los partidos
- Modelo entrenado (se guarda en memoria durante la sesión)

## Equipos disponibles

La aplicación automáticamente detecta todos los equipos disponibles en los archivos CSV.


## Notas técnicas

- El modelo usa Random Forest con 100 árboles
- Se utilizan los últimos 10 partidos para calcular estadísticas
- La precisión típica del modelo es del 50-60%
- Los datos se dividen en 80% entrenamiento y 20% validación