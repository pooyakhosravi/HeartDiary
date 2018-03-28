package indexing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import parsing.MyJsonParser;
import weather.WeatherAPI;

public class indexer {
	
	private static Gson gson = null;	
	
	public static void main(String[] args) {
		MyJsonParser parser = new MyJsonParser();
		initJsonBuilders();
		
		parser.loadFile("data/userdata.json");
		
		JsonObject indexedData = new JsonObject();
		JsonObject locData = new JsonObject();
		JsonObject hrData = new JsonObject();
		JsonObject actData = new JsonObject();
		
		//System.out.println(parser.data.entrySet());
		System.out.println(parser.data.getAsJsonObject("lifeLog").size());
		for(Map.Entry<String, JsonElement> o : parser.data.getAsJsonObject("lifeLog").entrySet()) {
			
			JsonObject val = o.getValue().getAsJsonObject();
			String key = o.getKey();
			System.out.println(key);
			try {
				addJsonChild(hrData, key, processWearable(val, key));
				addJsonChild(locData, key, processLocation(val, key));
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			
			
		}
		
		System.out.println(parser.data.getAsJsonObject("segment").size());
		
		for(Map.Entry<String, JsonElement> o : parser.data.getAsJsonObject("segment").entrySet()) {
			if(o.getValue().isJsonObject()) {
				JsonObject val = o.getValue().getAsJsonObject();
				String key = o.getKey();
				System.out.println(key);
				try {
			
					addJsonChild(actData , key, processActivity(val, key));
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
			
			
		}
		
		
		addJsonChild(indexedData, "heartrate", hrData);
		addJsonChild(indexedData, "location", locData);
		addJsonChild(indexedData, "activity", actData);
		
		writeTofile(indexedData, "indexed.json");
	}
	
	private static JsonObject processWearable(JsonObject val, String date) {
		JsonObject day = new JsonObject();
		if(val.has("wearable")) {
			if(val.getAsJsonObject("wearable").get("timely").isJsonArray()) {
				System.out.println(val.getAsJsonObject("wearable").getAsJsonArray("timely").size());
				Iterator<JsonElement> it = val.getAsJsonObject("wearable").getAsJsonArray("timely").iterator();
				int time = 0;
				it.next();
				JsonObject hrate = new JsonObject();
				while(it.hasNext()) {
					
					JsonElement elem = it.next();
					JsonArray arr = new JsonArray();
					if(elem.isJsonObject()) {
						arr = elem.getAsJsonObject().getAsJsonArray("heartRate");
						if(arr == null)
							continue;
						if(arr.size() == 10) {
							for(int i = 0; i < 10; i+=2) {
								addJsonProp(hrate, "" + (time + i /2), arr.get(i));
							}
						}else if(arr.size() < 5) {
							for(int i = 0; i < arr.size(); i++) {
								addJsonProp(hrate, "" + (time + i), arr.get(i));
							}
						}else {
							for(int i = 0; i < 5; i++) {
								addJsonProp(hrate, "" + (time + i), arr.get(i));
							}
						}
						
					}
//					System.out.print(getTime(time) + " : --" + arr.size() +  arr + " , ");
					time +=5;
//						if(elem.isJsonObject())
//							System.out.println(elem.getAsJsonObject().getAsJsonArray("heartRate"));
					addJsonChild(day, "rates", hrate);
				}
			}
			
			if(val.getAsJsonObject("wearable").get("timely").isJsonObject()) {
				Iterator<Map.Entry<String, JsonElement>> it = val.getAsJsonObject("wearable").getAsJsonObject("timely").entrySet().iterator();
				int time = 0;
				it.next();
				JsonObject hrate = new JsonObject();
				while(it.hasNext()) {
					Map.Entry<String, JsonElement> itTemp = it.next();
					JsonElement elem = itTemp.getValue();
					time = Integer.parseInt(itTemp.getKey()) * 5;
					JsonArray arr = new JsonArray();
					if(elem.isJsonObject()) {
						arr = elem.getAsJsonObject().getAsJsonArray("heartRate");
						if(arr.size() == 10) {
							for(int i = 0; i < 10; i+=2) {
								addJsonProp(hrate, "" + (time + i /2), arr.get(i));
							}
						}else if(arr.size() < 5) {
							for(int i = 0; i < arr.size(); i++) {
								addJsonProp(hrate, "" + (time + i), arr.get(i));
							}
						}else {
							for(int i = 0; i < 5; i++) {
								addJsonProp(hrate, "" + (time + i), arr.get(i));
							}
						}
						
					}
//					System.out.print(getTime(time) + " : --" + arr.size() +  arr + " , ");
					time +=5;
//						if(elem.isJsonObject())
//							System.out.println(elem.getAsJsonObject().getAsJsonArray("heartRate"));
					addJsonChild(day, "rates", hrate);
				}
			}
			
//			System.out.println();
//				System.out.println(val.getAsJsonObject("wearable").getAsJsonArray("timely").iterator());
		}else {
			System.out.println("Doesn't have wearable");
		}
		return day;
		
		
	}
	
	private static JsonObject processLocation(JsonObject val, String date) {
		JsonObject day = new JsonObject();
		WeatherAPI api = new WeatherAPI();
		if(val.has("mobile")) {
//			System.out.println(val.getAsJsonArray("mobile").size());
			if(val.get("mobile").isJsonArray()) {
				Iterator<JsonElement> it = val.getAsJsonArray("mobile").iterator();
				int time = 0;
				it.next();
				JsonObject loc = new JsonObject();
				String lat = "";
				String lng = "";
				while(it.hasNext()) {
					JsonObject temp = new JsonObject();
					JsonElement ele = it.next();
					JsonObject location = new JsonObject();
					if(ele.isJsonObject()) {
						JsonObject elem = ele.getAsJsonObject();
						location = elem.getAsJsonObject("location");
						lat = location.get("lat").getAsString();
						lng = location.get("lng").getAsString();
					}
//					System.out.print(getTime(time) + "(" + lat + ", " + lng  + ") , ");
					addJsonProp(temp, "lat", lat);
					addJsonProp(temp, "lng", lng);
					addJsonProp(temp, "temp", api.getTemp(lat, lng, date, time/5));
					addJsonProp(temp, "hum", api.getHumidity(lat, lng, date, time/5));
					
					
					addJsonChild(loc, "" + time, temp);
					time +=5;
					addJsonChild(day, "loc", loc);
				}
				System.out.println();
			}
			
			if(val.get("mobile").isJsonObject()) {
				Iterator<Map.Entry<String, JsonElement>> it = val.getAsJsonObject("mobile").entrySet().iterator();
				int time = 0;
				it.next();
				JsonObject loc = new JsonObject();
				String lat = "";
				String lng = "";
				while(it.hasNext()) {
					Map.Entry<String, JsonElement> itTemp = it.next();
					JsonObject temp = new JsonObject();
					
					JsonElement ele = itTemp.getValue();
					JsonObject location = new JsonObject();
					time = Integer.parseInt(itTemp.getKey()) * 5;
					if(ele.isJsonObject()) {
						JsonObject elem = ele.getAsJsonObject();
						location = elem.getAsJsonObject("location");
						lat = location.get("lat").getAsString();
						lng = location.get("lng").getAsString();
					}
//					System.out.print(getTime(time) + "(" + lat + ", " + lng  + ") , ");
					addJsonProp(temp, "lat", lat);
					addJsonProp(temp, "lng", lng);
					addJsonProp(temp, "temp", api.getTemp(lat, lng, date, time/5));
					addJsonProp(temp, "hum", api.getHumidity(lat, lng, date, time/5));
					
					addJsonChild(loc, "" + time, temp);
					time +=5;
					addJsonChild(day, "loc", loc);
				}
//				System.out.println();
			}

		}else {
//			System.out.println("Doesn't have mobile");
		}
		
		return day;
		
		
	}
	
	
	private static JsonObject processActivity(JsonObject val, String date) {
		JsonObject day = new JsonObject();
		JsonObject act = new JsonObject();
		if(val.isJsonObject()) {
			for(Map.Entry<String, JsonElement> o : val.entrySet()) {
				JsonObject temp = o.getValue().getAsJsonObject();
				String key = o.getKey();
//				System.out.println(Integer.parseInt(key));
				int time = Integer.parseInt(key) * 5;
				
//				System.out.println(temp);
				addJsonProp(act, "" + time, temp.getAsJsonArray("dailyActivitySet").get(0).getAsJsonObject().get("KEY_DAILYACTIVITY").getAsString());
				
			}
			addJsonChild(day, "activity", act);
		}else {
			System.out.println("Doesn't have activity");
		}
		
		return day;
		
		
	}
	
	private static String getTime(int i) {
		return i / 60 + ":" + i% 60;
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
}
