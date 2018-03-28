package indexing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import parsing.MyJsonParser;
import weather.GoogleGeolocationAPI;

class LocationIndexer {
	private static int DECIMAL_DEGREE = 3;
	private static Gson gson = null;
	
	MyJsonParser parser;
	JsonObject indexedData;

	GoogleGeolocationAPI googleAPI;
	
	public LocationIndexer() {
		parser = new MyJsonParser();
		initJsonBuilders();
		
		parser.loadFile("data/indexed.json");
		
		indexedData = new JsonObject();
		googleAPI = new GoogleGeolocationAPI();
	}
	
	public void run() {
		JsonObject data = parser.data;
		
		Set<String> keys = data.keySet();
		System.out.println(keys);
		
		JsonObject heartRates = data.getAsJsonObject("heartrate");

		JsonObject locations = data.getAsJsonObject("location");
		System.out.println("location keyset: " + locations.keySet());
				
		HashMap<String, JsonObject> parsedData = new HashMap<String, JsonObject>();
		
		for (String date : locations.keySet()) {
			if (heartRates.has(date)) {
				JsonObject loc = locations.getAsJsonObject(date).getAsJsonObject("loc");
				
				for (String min : loc.keySet()) {
					
					JsonObject json_min = loc.getAsJsonObject(min);
					
					String lat = json_min.get("lat").getAsString();
					String lat_3 = lat.substring(0, lat.indexOf(".") + 4);
					lat = lat.substring(0, lat.indexOf(".") + DECIMAL_DEGREE);
					
					String lng = json_min.get("lng").getAsString();
					String lng_3 = lng.substring(0, lng.indexOf(".") + 4);
					lng = lng.substring(0, lng.indexOf(".") + DECIMAL_DEGREE);
					
					this.googleAPI.loadData(lat_3, lng_3);
					JsonObject googleResults = this.googleAPI.jsonParser.data.getAsJsonArray("results").get(0).getAsJsonObject();
					String loc_name = googleResults.get("formatted_address").getAsString();
					
					String key = lat + "_" + lng;
					
					JsonObject hr_stats;
					if (!parsedData.containsKey(key)) {
						System.out.println("new entry to map!!!!");
						parsedData.put(key, new JsonObject());
						hr_stats = parsedData.get(key);
						hr_stats.addProperty("name", loc_name);
						hr_stats.addProperty("lat", lat);
						hr_stats.addProperty("lng", lng);
						hr_stats.addProperty("avg_hr", 0);
						hr_stats.addProperty("max_hr", Integer.MIN_VALUE);
						hr_stats.addProperty("max_time", "");
						hr_stats.addProperty("min_hr", Integer.MAX_VALUE);
						hr_stats.addProperty("min_time", "");
						hr_stats.addProperty("count", 0);
					} else
						hr_stats = parsedData.get(key);
					
					// get heart rate
					int minute = Integer.parseInt(min);
					for (int i = minute; i < minute + 5; ++i) {
						// Store max min avg count
						int avg_hr = hr_stats.get("avg_hr").getAsInt();
						int max_hr = hr_stats.get("max_hr").getAsInt();
						String max_time = hr_stats.get("max_time").getAsString();
						int min_hr = hr_stats.get("min_hr").getAsInt();
						String min_time = hr_stats.get("min_time").getAsString();
						int count = hr_stats.get("count").getAsInt();
						
						JsonObject rates = heartRates.getAsJsonObject(date).getAsJsonObject("rates");

						if (rates.has(Integer.toString(i))) {
							// add heart rate
							int cmp_hr = heartRates.getAsJsonObject(date).getAsJsonObject("rates").get(Integer.toString(i)).getAsInt();

							// calculate avg
							avg_hr = (avg_hr * count + cmp_hr) / (count + 1);
							count += 1;
							
							if (cmp_hr > max_hr) {
								max_hr = cmp_hr;
								max_time = getTimeCategory(i);
							}
							if (cmp_hr < min_hr) {
								min_hr = cmp_hr;
								min_time = getTimeCategory(i);
							}
						}
						hr_stats.addProperty("avg_hr", avg_hr);
						hr_stats.addProperty("max_hr", max_hr);
						hr_stats.addProperty("max_time", max_time);
						hr_stats.addProperty("min_hr", min_hr);
						hr_stats.addProperty("min_time", min_time);
						hr_stats.addProperty("count", count);
					}
				}
			}
		}
		
		for (String key : parsedData.keySet()) {
			this.indexedData.add(key, parsedData.get(key));
		}
		
		writeTofile(jsonBeauty(this.indexedData), "locIndex.json");
		
		System.out.println("done");
	}
	
	// time to time of day convert
	private static String getTimeCategory(int time) {
		if (time < 300)
			return "night";
		if (time < 720)
			return "morning";
		if (time < 1020)
			return "afternoon";
		if (time < 1300)
			return "evening";
		return "night";
	}
	
	private static void print(Object o){
		PrintWriter p = new PrintWriter(System.out);
		p.write(o.toString());
		p.write("\n");
		p.flush();
	}
	private static void writeTofile(Object o, String file_name){
		PrintWriter p;
		try {
			File f = new File(file_name);
			File parent = f.getParentFile();
			if(parent != null && !parent.exists())
				f.getParentFile().mkdirs();
			p = new PrintWriter(f);
			p.write(o.toString());
			p.flush();
			p.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}
	
	private static void initJsonBuilders(){
		  GsonBuilder gsonBuilder = new GsonBuilder();
		  
		  // Example for customizain serialization
		  // TODO does not seem to work
		  gsonBuilder.registerTypeAdapter(BigDecimal.class,  new JsonSerializer<BigDecimal>() {
			  	@Override
				public JsonElement serialize(BigDecimal paramT, Type paramType, JsonSerializationContext paramJsonSerializationContext) {
//					 Double value = paramT.doubleValue();
			  		// TODO remove the rounding to int just testing if this works
			  		 int value = paramT.intValue();
			         return new JsonPrimitive(value);
				}
		  });
		  gsonBuilder.registerTypeAdapter(Number.class,  new JsonSerializer<Number>() {
			  	@Override
				public JsonElement serialize(Number paramT, Type paramType, JsonSerializationContext paramJsonSerializationContext) {
			  		 int value = paramT.intValue();
			         return new JsonPrimitive(value);
				}
		  });


		  gson = gsonBuilder.setPrettyPrinting().create();
	}
		
	private String jsonBeauty (JsonObject json){
//		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json_beauty = gson.toJson(json);
		return json_beauty;
	}
	
	
	public static void addJsonChild(JsonObject parent, String prop, JsonElement child){
		if(child == null || child.equals(new JsonObject()))
			return;
		parent.add(prop, child);
	}
	
	public static void addJsonArray(JsonObject parent, String prop, JsonArray array){
		if(array == null || array.size() == 0)
			return;
		parent.add(prop, array);
	}
	
	public static void addJsonProp(JsonObject json, String prop, Object obj){
		if(obj == null)
			return;

		if(obj instanceof Boolean)
			json.addProperty(prop, (Boolean)obj);
		else if(obj instanceof Number){
			Number num = (Number)obj;
			json.addProperty(prop, num);
		}
		else if(obj instanceof String && ((String)obj).trim().length()>0)
			json.addProperty(prop, (String)obj);
		else if(obj instanceof Character)
			json.addProperty(prop, (Character)obj);
		else if(obj instanceof JsonPrimitive)
			json.addProperty(prop, ((JsonPrimitive)obj).getAsNumber());
		else if(obj instanceof JsonElement)
			json.addProperty(prop, ((JsonElement)obj).getAsString());
		else print("Undefined Object Type:" + obj.getClass().getName() + " Value:" + obj.toString());
	}
	
	public static void main(String [] args) {
		LocationIndexer li = new LocationIndexer();
		
		li.run();
		
	}
	
}
