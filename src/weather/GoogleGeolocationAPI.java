package weather;
import java.io.File;
import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import parsing.MyJsonParser;

/* 
 * Example URL:
 * https://maps.googleapis.com/maps/api/geocode/json?latlng=40.714224,-73.961452&key=AIzaSyCLgnQ3EvzaPDc1Ca9QrXUkHaCcEdfcjLQ
 * 
 * Caching to 3rd decimal place
 * https://en.wikipedia.org/wiki/Decimal_degrees
 */
public class GoogleGeolocationAPI {
	private static String GOOGLE_GEOLOCATION_BASE_URL = "https://maps.googleapis.com/maps/api/geocode/json?latlng=";
	private static String GOOGLE_GEOLOCATION_API_KEY = "AIzaSyDSIl-PmUEdMgAEIsAzHCXBdJkYIhqUqFs";
	//private static String GOOGLE_GEOLOCATION_API_KEY = "AIzaSyCLgnQ3EvzaPDc1Ca9QrXUkHaCcEdfcjLQ";
	
	// Caching to 3rd decimal place
	private static int DECIMAL_DEGREE = 4;
	public MyJsonParser jsonParser;
	
	public GoogleGeolocationAPI() {
		this.jsonParser = new MyJsonParser();
	}
	
	public String buildLocationURL(String lat, String lng) {
		return GOOGLE_GEOLOCATION_BASE_URL + lat + "," + lng + "&key=" + GOOGLE_GEOLOCATION_API_KEY;
	}
	
	public void loadData(String lat, String lng) {
		double la = Double.parseDouble(lat.substring(0, lat.indexOf(".") + DECIMAL_DEGREE));
		double ln = Double.parseDouble(lng.substring(0, lng.indexOf(".") + DECIMAL_DEGREE));
		
		String fname = "data/geolocation/" + Double.toString(la) + "_" + Double.toString(ln) + ".json";
		File f = new File(fname);
		
		if (f.exists()) {
			System.out.println("GoogleGeolocationAPI - Json Exists, loading file");
			this.jsonParser.loadFile(fname);
		} else {
			System.out.println("GoogleGeolocationAPI - Cannot find Json. Downloading file");
			this.jsonParser.downloadData(buildLocationURL(lat, lng), fname);
		}
	}
	
	public ArrayList<String> getParams(String lat, String lng) {
		ArrayList<String> params = new ArrayList<String> ();
		
		loadData(lat, lng);
		
		String stateShortName = null;
		String postalCode = null;
		
		JsonObject jsonObject = (JsonObject) this.jsonParser.data.getAsJsonArray("results").get(0);
		JsonArray jsonArray = jsonObject.getAsJsonArray("address_components");
		
		for (int i = 0; i < jsonArray.size(); ++i) {
			JsonObject component = (JsonObject) jsonArray.get(i);
			JsonArray type = component.getAsJsonArray("types");
			for (int j = 0; j < type.size(); ++j) {
				String t = type.get(j).getAsString();
				
				if (t.compareTo("administrative_area_level_1") == 0)
					stateShortName = component.get("short_name").getAsString();
				else if (t.compareTo("postal_code") == 0)
					postalCode = component.get("long_name").getAsString();
			}
		}
		params.add(stateShortName);
		params.add(postalCode);
		
		System.out.println("  State: " + stateShortName + "; PostalCode: " + postalCode);
		
		return params;
	}
}