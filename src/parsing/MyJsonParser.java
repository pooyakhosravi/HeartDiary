package parsing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class MyJsonParser {
	Gson gson;
	public JsonObject data;
	
	public MyJsonParser() {
		this.gson = new Gson();
		this.data = new JsonObject();
	}
	
	public void loadFile(String fname) {
		try {
			FileReader reader = new FileReader(fname);
			JsonElement root = new JsonParser().parse(reader);
			this.data = root.getAsJsonObject();
			
			reader.close();
		} catch (IOException | JsonIOException | JsonSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// url to download
	// fname to save to
	public void downloadData(String surl, String fname) {
		BufferedReader reader = null;
		try {
			URL url = new URL(surl);
			reader = new BufferedReader(new InputStreamReader(url.openStream()));
			
			StringBuffer buffer = new StringBuffer();
			int read;
			char [] chars = new char[1024];
			while ((read = reader.read(chars)) != -1)
				buffer.append(chars, 0, read);
			
			FileWriter file = new FileWriter(fname);
			file.write(buffer.toString());
			file.flush();
			
			loadFile(fname);
			
			reader.close();
			file.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void loadData(String surl) {
		BufferedReader reader = null;
		try {
			URL url = new URL(surl);
			reader = new BufferedReader(new InputStreamReader(url.openStream()));
			
			JsonElement root = new JsonParser().parse(reader);
			this.data = root.getAsJsonObject();
			
			reader.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
