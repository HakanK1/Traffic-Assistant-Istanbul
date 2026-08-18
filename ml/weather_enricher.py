import os
import glob
import pandas as pd
import requests

ENRICHED_FOLDER = os.path.join("data", "enriched")
FINAL_FOLDER = os.path.join("data", "model_ready")
WEATHER_CACHE_PATH = os.path.join("data", "istanbul_weather_archive.csv")

# Istanbul's center
LAT = 41.0082
LON = 28.9784

if not os.path.exists(FINAL_FOLDER):
    os.makedirs(FINAL_FOLDER)

def fetch_historical_weather(start_date="2019-01-01", end_date="2025-12-31"):
    if os.path.exists(WEATHER_CACHE_PATH):
        print("[+] Local weather data archive found, loading from the disk...")
        return pd.read_csv(WEATHER_CACHE_PATH, parse_dates=["time"])
    
    print(f"Fetching weather data from the Open-Meteo Archive API for {start_date} - {end_date}...")
    url = f"https://archive-api.open-meteo.com/v1/archive?latitude={LAT}&longitude={LON}&start_date={start_date}&end_date={end_date}&hourly=temperature_2m,precipitation,windspeed_10m&timezone=auto"
    
    response = requests.get(url)
    response.raise_for_status()
    data = response.json()
    
    hourly = data.get("hourly", {})
    weather_df = pd.DataFrame({
        "time": pd.to_datetime(hourly.get("time")),
        "temp": hourly.get("temperature_2m"),
        "precip": hourly.get("precipitation"),
        "wind": hourly.get("windspeed_10m")
    })
    
    os.makedirs("data", exist_ok=True)
    weather_df.to_csv(WEATHER_CACHE_PATH, index=False)
    print("[+] Weather data downloaded and saved to cache!")
    return weather_df

def main():
    print("--- PAST WEATHER DATA INTEGRATION AND SECURE TRANSFER ARE BEING INITIATED ---")
    

    parquet_files = glob.glob(os.path.join(ENRICHED_FOLDER, "*.parquet"))
    if not parquet_files:
        print("[-] couldn't find any parquet file in the data/enriched folder!")
        return
        
    weather_df = fetch_historical_weather("2019-01-01", "2025-12-31")
    weather_df['time'] = pd.to_datetime(weather_df['time'])

    for file_path in parquet_files:
        file_name = os.path.basename(file_path)
        target_path = os.path.join(FINAL_FOLDER, file_name)
        
        # if it already exists in model_ready, skip (Idempotent design)
        if os.path.exists(target_path):
            print(f"[!] Skipping: {file_name} already exists in model_ready.")
            continue
            
        print(f"[*] Processing: {file_name}...")
        df = pd.read_parquet(file_path)
        
        time_column = 'DATE_TIME' if 'DATE_TIME' in df.columns else [c for c in df.columns if pd.api.types.is_datetime64_any_dtype(df[c]) or 'time' in c.lower()][0]
        df[time_column] = pd.to_datetime(df[time_column])
        
        df_merged = pd.merge(df, weather_df, left_on=time_column, right_on='time', how='inner')
        
        columns_to_keep = ['GEOHASH', 'AVERAGE_SPEED', 'hour', 'dayOfWeek', 'isHoliday', 'temp', 'precip', 'wind']
        df_final = df_merged[[col for col in columns_to_keep if col in df_merged.columns]]
        
        # Saving it into the new folder.
        df_final.to_parquet(target_path, engine='pyarrow', index=False)
        print(f"[+] Safely recorded! -> {target_path} (Columns: {list(df_final.columns)})")

    print("\n--- ALL FILES HAVE BEEN SUCCESSFULLY COPIED TO THE MODEL_READY DIRECTORY ---")

if __name__ == "__main__":
    main()