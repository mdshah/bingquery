package com.servlet.explore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

//Download and add this library to the build path.
import org.apache.commons.codec.binary.Base64;

public class BingQuery {

	private double precision;
	private String query; 
	private static String accountKey = "PJD4UwjOC50tzY6BN95L7ftRuQ5EMavSK14aCsiiEjc=";
	private String bingUrl;

	public static void main(String[] args) throws IOException {

		double precision = 0.7;
		String query = "gates";

		if(args.length > 0)
			accountKey = args[0];
		if(args.length > 1)
			precision = Double.parseDouble(args[1]);
		if(args.length > 2)
			query = args[2];

		BingQuery bq = new BingQuery(query, precision);
		bq.startQuery(); 
	}

	public BingQuery(String query, double precision) {
		this.precision = precision;
		this.query = query;
		this.bingUrl = "https://api.datamarket.azure.com/Data.ashx/Bing/SearchWeb/v1/Web?Query=%27"+formatQuery(query)+"%27&$top=10&$format=JSON";
	}

	private String formatQuery(String query2) {
		String queryTemp = "";
		String[] queryWords = query2.split(" ");
		if(queryWords.length > 1) {
			for(int i = 0; i < queryWords.length - 1; i++) {
				queryTemp += queryWords[i] + "%20";
			}
			queryTemp += queryWords[queryWords.length - 1];
		}
		else queryTemp = query2;
		return queryTemp;
	}

	public void startQuery() {
		printHeader();
		retrieveResults();		
	}

	public void printHeader() {
		System.out.println("Parameters: ");
		System.out.println("Client key = " + accountKey);
		System.out.println("Query      = " + query);
		System.out.println("Precision  = " + precision);
		System.out.println("URL: " + bingUrl);
		System.out.println("Total no of results : 10");
	}

	public void retrieveResults() {

		try {
			byte[] accountKeyBytes = Base64.encodeBase64((accountKey + ":" + accountKey).getBytes());
			String accountKeyEnc = new String(accountKeyBytes);

			URL url = new URL(bingUrl);
			URLConnection urlConnection = url.openConnection();
			urlConnection.setRequestProperty("Authorization", "Basic " + accountKeyEnc);

			InputStream inputStream = (InputStream) urlConnection.getContent();		
			BufferedReader in = new BufferedReader(new InputStreamReader(inputStream,"UTF-8"));
			String inputLine;
			StringBuffer output=new StringBuffer("");
			while ((inputLine = in.readLine()) != null)
				output.append((inputLine));
			in.close();

			String json = output.toString();
			JSONObject jo = new JSONObject(json);
			printResults(jo);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void printResults(JSONObject jo) {
		Scanner sc = new Scanner(System.in); 
		try {
			jo = jo.getJSONObject("d");
			JSONArray ja = jo.getJSONArray("results");
			System.out.println("Bing Search Results: ");
			System.out.println("======================");
			int no = 0; 
			if (ja.length() < 10) {
				System.exit(0);
			}
			for (int i = 1; i <= ja.length(); i++)
			{
				System.out.println("Result " + i);
				System.out.println("[");
				JSONObject resultObject = ja.getJSONObject(i-1);
				System.out.println("URL: " + resultObject.get("Url"));
				System.out.println("Title: "+ resultObject.get("Title"));
				System.out.println("Summary: " + resultObject.get("Description"));
				System.out.println("]");
				System.out.println();
				System.out.println("Relevant (Y/N)?");
				String relevant = sc.next();
				boolean answered = false; 
				while(!answered) {
					if(relevant.equalsIgnoreCase("n")) {
						answered = true;
						no++;
					}						
					else if(relevant.equalsIgnoreCase("y")) 
						answered = true;
					else {
						System.out.println("Invalid input. Only type y or n.");
						relevant = sc.next();
					}
				}
			}	
			System.out.println("=======================");
			System.out.println("FEEDBACK SUMMARY");
			System.out.println("Query " + query);
			System.out.println("Precision " + computeRelevancy(no));
			if(computeRelevancy(no) >= precision) {
				System.out.println("Desired precision reached, done");
				System.exit(0);
			}
			else if (computeRelevancy(no) == 0) {
				System.out.println("There were no relevant documents. System will now exit.");
				System.exit(0);
			}
			else {
				System.out.println("Still below the desired precision of " + precision);
				System.out.println("Indexing results ... ");
				System.out.println("Indexing results ... ");
				String augmented = augmentQueryWith();
				System.out.println("Augmenting by " + augmented);
				query += " " + augmented;
				updateBingURL();
				printHeader();
				retrieveResults();
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public double computeRelevancy(int no) {
		return (10 - no) / 10.0;		
	}

	private void updateBingURL() {
		this.bingUrl = "https://api.datamarket.azure.com/Data.ashx/Bing/SearchWeb/v1/Web?Query=%27"+formatQuery(query)+"%27&$top=10&$format=JSON";
	}

	private String augmentQueryWith() {

		return "temp";
	}

}