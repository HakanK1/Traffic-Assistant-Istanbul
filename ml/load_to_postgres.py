import pandas as pd
import glob
import os
from sqlalchemy import create_engine
import gc
from tqdm import tqdm # progress bar

print("--- POSTGRESQL DATABASE UPDATE STARTING ---")
# connecting to PostgreSQL in Docker (Docker must be OPEN!)
engine = create_engine('postgresql://admin:12345@localhost:5432/traffic_db')

# reading our prepared 'model_ready' folder.
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(BASE_DIR, "data", "model_ready") 
parquet_files = glob.glob(os.path.join(DATA_DIR, "*.parquet"))

if not parquet_files:
    print(f"[-] ERROR: {DATA_DIR} no file found to read in the directory!")
    exit()

print(f"[*] Total {len(parquet_files)} enriched files being read...")

# Target columns in the new structure
required_columns = ['GEOHASH', 'AVERAGE_SPEED', 'hour', 'dayOfWeek', 'isHoliday', 'temp', 'precip', 'wind']
subtotals = []

print("[*] Data is being processed in chunks (RAM-Friendly Map-Reduce)...")
for file in tqdm(parquet_files, desc="Files Processing"):
    # loading only required columns
    df_temp = pd.read_parquet(file, columns=required_columns)

    # Grouping by region, day, hour and holiday status for each file
    group = df_temp.groupby(['GEOHASH', 'dayOfWeek', 'hour', 'isHoliday']).agg(
        total_speed=('AVERAGE_SPEED', 'sum'),
        total_temp=('temp', 'sum'),
        total_precip=('precip', 'sum'),
        total_wind=('wind', 'sum'),
        record_count=('AVERAGE_SPEED', 'count')
    ).reset_index()

    subtotals.append(group)
    
    # Cleaning RAM immediately
    del df_temp
    gc.collect()

print("\n[*] All summaries are being merged and final averages are being calculated...")

# merging small summaries
df_all = pd.concat(subtotals, ignore_index=True)
del subtotals
gc.collect()

# getting the total sums of all data
final_group = df_all.groupby(['GEOHASH', 'dayOfWeek', 'hour', 'isHoliday']).agg(
    general_total_speed=('toplam_hiz', 'sum'),
    general_total_temp=('toplam_temp', 'sum'),
    general_total_precip=('toplam_precip', 'sum'),
    general_total_wind=('toplam_wind', 'sum'),
    general_record_count=('kayit_sayisi', 'sum')
).reset_index()

del df_all
gc.collect()

# Calculating Real Averages
final_group['average_speed'] = final_group['genel_toplam_hiz'] / final_group['genel_kayit_sayisi']
final_group['avg_temp'] = final_group['genel_toplam_temp'] / final_group['genel_kayit_sayisi']
final_group['avg_precip'] = final_group['genel_toplam_precip'] / final_group['genel_kayit_sayisi']
final_group['avg_wind'] = final_group['genel_toplam_wind'] / final_group['genel_kayit_sayisi']

# Deleting garbage/unused columns
final_group.drop(columns=['genel_toplam_hiz', 'genel_toplam_temp', 'genel_toplam_precip', 'genel_toplam_wind', 'genel_kayit_sayisi'], inplace=True)

# Java Entity Mapping (Following database naming standards)
final_group.rename(columns={
    'GEOHASH': 'geohash_code',
    'dayOfWeek': 'day_of_week',
    'hour': 'hour_of_day',
    'isHoliday': 'is_holiday'
}, inplace=True)

print(f"[*] Total {len(final_group):,} unique records being written to PostgreSQL...")

# if_exists='replace' >>> deleting the old one and creating a new one
final_group.to_sql('traffic_history', engine, if_exists='replace', index=False)

print("\n✅ Perfect! Database successfully updated with the new 7-parameter structure.")