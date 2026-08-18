import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as L from 'leaflet';
import * as geohash from 'ngeohash';
import { TrafficService, RoutePredictionRequest } from './services/traffic';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App implements OnInit {
  
  private map!: L.Map;
  
  private startPoint: L.LatLng | null = null;
  private endPoint: L.LatLng | null = null;
  private routingControl: any = null;
  private startMarker: L.CircleMarker | null = null;
  private endMarker: L.CircleMarker | null = null;

  constructor(private trafficService: TrafficService) {}

  ngOnInit(): void {
    this.initMap();
  }

  private initMap(): void {
    this.map = L.map('map').setView([41.0082, 28.9784], 11);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 18,
      attribution: '© OpenStreetMap contributors'
    }).addTo(this.map);

    this.map.on('click', (e: L.LeafletMouseEvent) => {
      if (this.startPoint && this.endPoint) {
        this.clearRoute();
      }

      if (!this.startPoint) {
        this.startPoint = e.latlng;
        
        this.startMarker = L.circleMarker([this.startPoint.lat, this.startPoint.lng], {
          color: '#27ae60', fillColor: '#2ecc71', fillOpacity: 0.9, radius: 8
        }).bindPopup('<b>Point A</b> (Starting point)').addTo(this.map).openPopup();
          
      } else if (!this.endPoint) {
        this.endPoint = e.latlng;
        
        this.endMarker = L.circleMarker([this.endPoint.lat, this.endPoint.lng], {
          color: '#c0392b', fillColor: '#e74c3c', fillOpacity: 0.9, radius: 8
        }).bindPopup('<b>Point B</b> (Destination)').addTo(this.map).openPopup();

        this.drawRoute();
      }
    });
  }

  private clearRoute(): void {
    this.startPoint = null;
    this.endPoint = null;
    
    if (this.startMarker) this.map.removeLayer(this.startMarker);
    if (this.endMarker) this.map.removeLayer(this.endMarker);
    
    if (this.routingControl) {
      this.map.removeControl(this.routingControl);
      this.routingControl = null;
    }
  }

  private async drawRoute() {
    if (!this.startPoint || !this.endPoint) return;

    try {
      (window as any).L = L;
      await import('leaflet-routing-machine');

      const routingFactory = (L as any).Routing;
      if (routingFactory && routingFactory.control) {
        this.routingControl = routingFactory.control({
          waypoints: [
            L.latLng(this.startPoint.lat, this.startPoint.lng),
            L.latLng(this.endPoint.lat, this.endPoint.lng)
          ],
          routeWhileDragging: false,
          addWaypoints: false,
          show: false, 
          lineOptions: {
            styles: [{ color: '#3498db', opacity: 0.8, weight: 6 }] 
          }
        }).addTo(this.map);
      } else {
        throw new Error("Factory not found");
      }
    } catch (err) {
      console.error("Leaflet Routing Machine could not be loaded!", err);
      if (this.endMarker) {
        this.endMarker.bindPopup(`<div style="color: #c0392b; font-weight:bold;">The route-drawing module could not be loaded!</div>`).openPopup();
      }
      return;
    }

    this.routingControl.on('routesfound', async (e: any) => {
      const routes = e.routes;
      const coordinates = routes[0].coordinates;
      
      const uniqueGeohashes = new Set<string>();
      coordinates.forEach((coord: any) => {
        const hash = geohash.encode(coord.lat, coord.lng, 6);
        uniqueGeohashes.add(hash);
      });

      const routeGeohashes = Array.from(uniqueGeohashes);
      
      if (this.endMarker) {
        this.endMarker.bindPopup(`
          <div style="text-align: center; color: #7f8c8d;">
            Weather Conditions and AI Analysis in Progress...<br>
            <small>(${routeGeohashes.length} areas are being analyzed)</small>
          </div>
        `).openPopup();
      }

      try {
        const now = new Date();
        const currentHour = now.getHours();
        
        const jsDay = now.getDay();
        const backendDay = jsDay === 0 ? 6 : jsDay - 1; 
        
        const isHoliday = (backendDay === 5 || backendDay === 6) ? 1 : 0; 

        // Dynamic weather update (added)
        // Getting the current weather conditions for the destination point using the Open-Meteo API.
       
        const targetLat = this.endPoint!.lat;
        const targetLng = this.endPoint!.lng;
        
        const weatherResponse = await fetch(`https://api.open-meteo.com/v1/forecast?latitude=${targetLat}&longitude=${targetLng}&current=temperature_2m,precipitation,wind_speed_10m`);
        const weatherData = await weatherResponse.json();
        
        const currentTemp = weatherData.current.temperature_2m;
        const currentPrecip = weatherData.current.precipitation;
        const currentWind = weatherData.current.wind_speed_10m;

        const requestPayload: RoutePredictionRequest = {
          geohashList: routeGeohashes,
          hour: currentHour,
          dayOfWeek: backendDay,
          isHoliday: isHoliday,
          temp: currentTemp,
          precip: currentPrecip,
          wind: currentWind
        };

        this.trafficService.getRoutePrediction(requestPayload).subscribe({
          next: (response: any) => {
            const etaMinutes = response.estimated_time_minutes;
            const segmentCount = response.geohash_count;

            if (this.endMarker) {
                this.endMarker.bindPopup(`
                  <div style="font-family: Arial, sans-serif; text-align: center;">
                    <span style="font-size: 14px; color: #34495e;">Estimated Arrival Time</span><br><br>
                    <span style="color: #27ae60; font-weight: bold; font-size: 26px;">${etaMinutes} Minutes</span><br><br>
                    <span style="font-size: 11px; color: #7f8c8d;">
                      Current Weather: ${currentTemp}°C, Wind: ${currentWind} km/s<br>
                      Analyzed Areas: ${segmentCount}
                    </span>
                  </div>
                `).openPopup();
            }
          },
          error: (error: any) => {
             console.error("Rota API Hatası:", error);
             if (this.endMarker) {
                 this.endMarker.bindPopup(`<div style="color: #c0392b; font-weight:bold;">Calculation Error! Is the Backend Running?</div>`).openPopup();
             }
          }
        });

      } catch (err) {
         console.error("Weather API Error:", err);
         if (this.endMarker) {
             this.endMarker.bindPopup(`<div style="color: #c0392b; font-weight:bold;">Failed to Retrieve Weather Data! Please Check Your Internet Connection.</div>`).openPopup();
         }
      }
    });
  }
}