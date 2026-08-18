# AI-Powered Istanbul Traffic Route Predictor

This repository contains an end-to-end full-stack application that predicts route durations in Istanbul based on historical traffic data and real-time weather conditions. The system is built with a microservices approach, utilizing an XGBoost machine learning model integrated directly into a Java Spring Boot backend, and visualized through an interactive Angular interface.

## System Architecture & Tech Stack

*   **Machine Learning:** Python, Pandas, XGBoost, ONNX
*   **Backend:** Java 25, Spring Boot, Maven, ONNX Runtime
*   **Frontend:** Angular 22, TypeScript, Leaflet, Leaflet Routing Machine
*   **Database:** PostgreSQL
*   **DevOps:** Docker, Docker Compose

## Key Features

*   **Geohash-Based Traffic Analysis:** The system maps raw geographical coordinates into Geohash Level 6 grids to analyze traffic flow precisely and efficiently.
*   **Embedded AI Execution:** The XGBoost model is exported to `.onnx` format. This allows the Java Spring Boot backend to load and run predictions natively using `onnxruntime`, eliminating the need for a separate Python inference server and significantly reducing latency.
*   **Real-Time Weather Integration:** The backend dynamically fetches live weather data (temperature and wind parameters) for the destination coordinates via the Open-Meteo API to adjust prediction weights.
*   **Interactive Routing:** Users can select start and destination points on a Leaflet map. The application draws the route, extracts the exact coordinates along the path, and returns the Estimated Time of Arrival (ETA).
*   **Fully Containerized:** The Database, Backend API, and Frontend application are orchestrated through Docker Compose for standardized deployment.

## Machine Learning Pipeline

The prediction model was trained using historical traffic data provided by the Istanbul Metropolitan Municipality (IBB). 

1.  **Data Preprocessing:** Removed redundant columns and converted raw GPS coordinates into Geohash codes. Extracted temporal features (`hour_of_day`, `day_of_week`, `is_holiday`).
2.  **Model Evaluation:** Tested multiple algorithms including Naive Baseline, Ridge Regression, Random Forest, LightGBM, and XGBoost.
3.  **Final Selection:** XGBoost was selected as the final model due to its optimal balance of inference speed and low RMSE/MAE metrics. 

*Note: The raw training datasets (`.parquet` files) and large model files are excluded from this repository due to size constraints. They can be found in the Releases section.*

## Project Structure

*   `/api` - Java Spring Boot backend source code, REST controllers, and ONNX model resources.
*   `/frontend` - Angular application, Leaflet map configuration, and UI components.
*   `/ml` - Python environment containing data cleaning scripts, model training notebooks, and `requirements.txt`.
*   `docker-compose.yml` - Container orchestration configuration.

## Installation and Setup

### Prerequisites
*   Docker and Docker Compose must be installed on your system ahead of time.

### Running the Application

Open your terminal and get in the folder wherever you prefer installing the project.
1. Clone the repository:
    
   `git clone [https://github.com/HakanK1/Traffic-Assistant-Istanbul.git](https://github.com/HakanK1/Traffic-Assistant-Istanbul.git)`
   `cd Traffic-Assistant-Istanbul`

2. Ensure that docker is active (notice the text "Engine running" on the bottom left in the app indicating active status), then build and start the infrastructure:
     `docker-compose up -d --build`
   
3. Access the services:
     Copy and paste the address below to run the frontend and access the application with everything (docker must be running everytime you want to run the application).
     `Frontend Interface: http://localhost:4200`
     `Backend API: http://localhost:8080`
   
4. To stop the application and remove containers, run: `docker-compose down`

## Optional: Machine Learning Development Setup

If you wish to retrain the XGBoost model or explore the data engineering pipeline, you need to set up the Python environment locally.

After orchestrating everything with docker:
1. Navigate to the Machine Learning directory:
   `cd ml`

2. Create a virtual environment:
    `python -m venv venv`

3. Activate the virtual environment:
    Windows: `.\venv\Scripts\activate`
    Linux/Mac: `source venv/bin/activate`

4. install the required dependencies:
   `pip install -r requirements.txt`
