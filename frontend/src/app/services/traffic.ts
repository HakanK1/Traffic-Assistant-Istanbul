import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// 7 Parameter model
export interface LivePredictionRequest {
  geohashString: string;
  hour: number;
  dayOfWeek: number;
  isHoliday: number;
  temp: number;
  precip: number;
  wind: number;
}

// 7 Parameter model for route prediction
export interface RoutePredictionRequest {
  geohashList: string[];
  hour: number;
  dayOfWeek: number;
  isHoliday: number;
  temp: number;
  precip: number;
  wind: number;
}

@Injectable({
  providedIn: 'root'
})
export class TrafficService {
  
  private apiUrl = 'http://localhost:8080/api/v1/traffic/predict';
  private routeApiUrl = 'http://localhost:8080/api/v1/traffic/predict-route'; 

  constructor(private http: HttpClient) { }

  getPrediction(requestData: LivePredictionRequest): Observable<any> {
    return this.http.post(this.apiUrl, requestData);
  }

  getRoutePrediction(requestData: RoutePredictionRequest): Observable<any> {
    return this.http.post(this.routeApiUrl, requestData);
  }
}