package weather;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import parsing.MyJsonParser;


/*
- Personicle Data - 
Date format: MMDD_YYYY
Time: int 1 -> 288 5 minute time intervals

- API - 
Example URL:
http://api.wunderground.com/api/94ecb4f2dae810ac/history_20180304/q/CA/irvine.json
http://api.wunderground.com/api/94ecb4f2dae810ac/history_20180304/q/CA/92617.json

https://maps.googleapis.com/maps/api/geocode/json?latlng=40.714224,-73.961452&key=AIzaSyCLgnQ3EvzaPDc1Ca9QrXUkHaCcEdfcjLQ
*/




public class WeatherAPI {
	private static String WUNDERGROUND_BASE_URL = "http://api.wunderground.com/api/";
	private static String WUNDERGROUND_API_KEY = "94ecb4f2dae810ac";
	
	MyJsonParser jsonParser;
	GoogleGeolocationAPI geolocationAPI;
	
	public WeatherAPI() {
		this.jsonParser = new MyJsonParser();
		this.geolocationAPI = new GoogleGeolocationAPI();
	}
	
	public int calculateDifference(int h, int m, int h_cmp, int m_cmp) {
		return (h * 60 + m) - (h_cmp * 60 + m_cmp);
	}
	
	public float getClosestTime(JsonArray jsonArray, int time) {
		int hour = convertHour(time);
		int min = convertMinute(time);
		
		int min_dif = Integer.MAX_VALUE;
		float temp_f = 0;
		int humidity = 0;
		
		for (int i = 0; i < jsonArray.size(); ++i) {
			JsonObject e = (JsonObject) jsonArray.get(i);
			// Assume tzname : "America/Los_Angeles"
			int hour_cmp = e.getAsJsonObject("date").get("hour").getAsInt();
			int min_cmp = e.getAsJsonObject("date").get("min").getAsInt();
			if (hour >= hour_cmp - 1 && hour <= hour_cmp + 1) {
				int dif_cmp = calculateDifference(hour, min, hour_cmp, min_cmp);
				if (Math.abs(dif_cmp) < Math.abs(min_dif)) {
					min_dif = dif_cmp;
					
					temp_f = e.get("tempi").getAsFloat();
					humidity = e.get("hum").getAsInt();
				}
			}
		}
		
		System.out.println("  Temp: " + temp_f + " f");
		
		return temp_f;
	}
	
	public float getHumidityClosestTime(JsonArray jsonArray, int time) {
		int hour = convertHour(time);
		int min = convertMinute(time);
		
		int min_dif = Integer.MAX_VALUE;
		float temp_f = 0;
		int humidity = 0;
		
		for (int i = 0; i < jsonArray.size(); ++i) {
			JsonObject e = (JsonObject) jsonArray.get(i);
			// Assume tzname : "America/Los_Angeles"
			int hour_cmp = e.getAsJsonObject("date").get("hour").getAsInt();
			int min_cmp = e.getAsJsonObject("date").get("min").getAsInt();
			if (hour >= hour_cmp - 1 && hour <= hour_cmp + 1) {
				int dif_cmp = calculateDifference(hour, min, hour_cmp, min_cmp);
				if (Math.abs(dif_cmp) < Math.abs(min_dif)) {
					min_dif = dif_cmp;
					
					temp_f = e.get("tempi").getAsFloat();
					humidity = e.get("hum").getAsInt();
				}
			}
		}
		
		System.out.println("  Temp: " + temp_f + " f");
		
		return humidity;
	}
	
	//public String getTemp(String state, String city, String date, int time) {
	public float getTemp(String lat, String lng, String date, int time) {
		ArrayList<String> params = this.geolocationAPI.getParams(lat, lng);
		String stateShortName = params.get(0);
		String postalCode = params.get(1);
		
		loadData(stateShortName, postalCode, date);

		JsonArray jsonArray = this.jsonParser.data.getAsJsonObject("history").getAsJsonArray("observations");
		
		return this.getClosestTime(jsonArray, time);
	}
	
	//public String getTemp(String state, String city, String date, int time) {
	public float getHumidity(String lat, String lng, String date, int time) {
		ArrayList<String> params = this.geolocationAPI.getParams(lat, lng);
		String stateShortName = params.get(0);
		String postalCode = params.get(1);
		
		loadData(stateShortName, postalCode, date);

		JsonArray jsonArray = this.jsonParser.data.getAsJsonObject("history").getAsJsonArray("observations");
		
		return this.getHumidityClosestTime(jsonArray, time);
	}
	
	/* 1  : 12:00 am - 12:04 am
	 * 2  : 12:05 am - 12:10 am <- shouldnt it be 12:09..
	 * 12 : 12:55 am - 12:59 am
	 * 288: 23:55 pm - 23.59 pm
	 * ASK about time zone recorded in jordan's system 
	 */
	public int convertHour(int personicleIndex) {
		return (personicleIndex - 1) / 12;
	}
	
	public int convertMinute(int personicleIndex) {
		return ((personicleIndex - 1) * 5) % 60 + 2;
	}
	
	public void loadData(String state, String postalCode, String date) {
		String fname = "data/weather/" + state + "_" + postalCode + "_" + date + ".json";
		File f = new File(fname);
		
		if (f.exists()) {
			System.out.println("WeatherAPI - Json Exists, loading file");
			this.jsonParser.loadFile(fname);
		} else {
			System.out.println("WeatherAPI - Cannot find Json. Downloading file");
			this.jsonParser.downloadData(buildHistoryURL(state, postalCode, date), fname);
		}
	}

	public String buildHistoryURL(String state, String city, String date) {
		String [] newDate = date.split("_");
		return WUNDERGROUND_BASE_URL + WUNDERGROUND_API_KEY + "/history_" 
				+ newDate[1] + newDate[0] + "/q/" + state + "/" + city + ".json";
	}
	
	public static void main(String [] args) {
		WeatherAPI api = new WeatherAPI();
		
		String personicle_lat = "33.6433947";
		String personicle_lng = "-117.8423435";
		String personicle_date = "0302_2018";
		int personicle_time = 200;
		
		api.getTemp(personicle_lat, personicle_lng, personicle_date, personicle_time);
	}
}


