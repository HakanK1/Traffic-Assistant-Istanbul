import os
import pandas as pd
import requests
import re
import holidays

# Folder structure
ENRICHED_DIR = os.path.join("data", "enriched")
TEMP_FILE = os.path.join(ENRICHED_DIR, "temp_raw_data.csv")
API_URL = "https://data.ibb.gov.tr/api/3/action/package_show?id=hourly-traffic-density-data-set"

if not os.path.exists(ENRICHED_DIR):
    os.makedirs(ENRICHED_DIR)

# initialize Turkey holiday calendar from 2019 to 2026
tr_holidays = holidays.Turkey(years=range(2019, 2027))

def download_file(url, filename):
    print(f"[*] downloading... (The time may vary depending on the internet speed.)")
    response = requests.get(url, stream=True)
    response.raise_for_status()
    with open(filename, 'wb') as f:
        for chunk in response.iter_content(chunk_size=8192):
            f.write(chunk)

def process_and_enrich(month_key, url):
    destination_path = os.path.join(ENRICHED_DIR, f"trafik_verisi_{month_key}.parquet")
    
    if os.path.exists(destination_path):
        print(f"[!] {month_key} is already processed, skipping.")
        return

    print(f">>> Processing: {month_key} <<<")
    download_file(url, TEMP_FILE)
    
    print(f"[*] {month_key} is being processed with pandas and holiday values are being calculated.")
    df = pd.read_csv(TEMP_FILE, sep=',') 
    
    time_column = 'DATE_TIME' if 'DATE_TIME' in df.columns else df.columns[0]
    df[time_column] = pd.to_datetime(df[time_column])
    
    df['hour'] = df[time_column].dt.hour
    df['dayOfWeek'] = df[time_column].dt.dayofweek
    
    # holiday flag injection
    df['isHoliday'] = df[time_column].dt.date.apply(lambda x: 1 if x in tr_holidays else 0)
    
    # keeping the time column because it may be useful for weather integration
    necessary_columns = ['GEOHASH', 'AVERAGE_SPEED', 'hour', 'dayOfWeek', 'isHoliday', time_column]
    df_clean = df[[col for col in necessary_columns if col in df.columns]]
    
    df_clean.to_parquet(destination_path, engine='pyarrow', index=False)
    print(f"[+] Enriching and contracting successful: {destination_path} (Size: {os.path.getsize(destination_path) / (1024*1024):.2f} MB)")
    
    # cleaning up the garbage
    if os.path.exists(TEMP_FILE):
        os.remove(TEMP_FILE)
        print(f"[+] Geçici CSV çöpü imha edildi.\n")

def main():
    print("--- FULLY AUTOMATIC DATA ENRICHING IS BEGINNING ---")
    try:
        res = requests.get(API_URL)
        res.raise_for_status()
        data = res.json()
    except Exception as e:
        print(f"[-] API Connection Error: {e}")
        return

    csv_links = [r for r in data["result"]["resources"] if r["format"].lower() == "csv"]
    print(f"[+] {len(csv_links)} monthly datasets found in total. Scanning...\n")

    for r in csv_links:
        url = r["url"]
        match = re.search(r'(20[1-2]\d)(0[1-9]|1[0-2])\.csv', url.lower())
        if not match:
            continue
            
        year, month = match.groups()
        month_key = f"{year}_{month}"
        
        try:
            process_and_enrich(month_key, url)
        except Exception as e:
            print(f"[-] Error while processing: {month_key} {e}\n")

    print("--- ALL MONTHS HAVE BEEN ENRICHED, SUCCESS! ---")

if __name__ == "__main__":
    main()