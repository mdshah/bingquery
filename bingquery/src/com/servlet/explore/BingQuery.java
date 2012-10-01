package com.servlet.explore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;

//Download and add this library to the build path.
import org.apache.commons.codec.binary.Base64;

public class BingQuery {

	private double precision;
	private String query; 
	private static String accountKey = "PJD4UwjOC50tzY6BN95L7ftRuQ5EMavSK14aCsiiEjc=";
	private String bingUrl;
	private ArrayList<String> relDoc;
	private ArrayList<String> nonRelDoc;
	private HashMap<String, Double> q_vect;
	private HashMap<String, Integer> invListRel;
	private HashMap<String, Integer> invListNonRel;
	private static final double BETA = 0.75;
	private static final double GAMMA = 0.25;

	public static void main(String[] args) throws IOException {

		double precision = 0.7;
		String query = "snow leopard";

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
		this.relDoc = new ArrayList<String>();
		this.nonRelDoc = new ArrayList<String>();
		this.q_vect = new HashMap<String, Double>();
		this.invListRel = new HashMap<String, Integer>();
		this.invListNonRel = new HashMap<String, Integer>();
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
		// addQueryTerms();
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
						nonRelDoc.add(resultObject.get("Title").toString());
						no++;
					}						
					else if(relevant.equalsIgnoreCase("y")) {
						relDoc.add(resultObject.get("Title").toString());
						answered = true;
					}
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

	/*	private void addQueryTerms() {
		for(String s : query.split(" ")) {
			s = s.toLowerCase().trim().replaceAll("[^\\p{L}\\p{N}]", "");
			if(!s.equals("")) { //and not one of the stop words
				q_vect.put(s, 0.0);
			}
		}	
	} */

	private String augmentQueryWith() {
		addTermsToQVect();
		computeWeightsForOriginalQuery();
		computeWeightsForQvect();
		String[] topTerms = findTopKterms();	
		String augment = "";
		int j = 0; 
		for(int i = 0; i < topTerms.length - 1; i++) {
			if(!alreadyQuery(topTerms[i]) && j < 2) {
				augment += topTerms[i] + " ";
				j++;
			}
		}
		if(!alreadyQuery(topTerms[topTerms.length - 1]) && j < 2)
			augment += topTerms[topTerms.length - 1]; 

		return augment;
	}



	private String[] findTopKterms() {

		/*		ArrayList<String> keys = new ArrayList<String>();
		ArrayList<Double> values = new ArrayList<Double>();

		for(String d : q_vect.keySet()) {
			values.add(q_vect.get(d));
			keys.add(d);
		}

		Collections.sort(values);

		String[] array = new String[values.size()];
		for(int i = 0; i < keys.size(); i++) {
			int index = findIndex(values.get(i), values);
			array[index] = keys.get(i); 
		} */


		String[] array = new String[2];
		String top = "";
		String second = "";
		double top_v = 0;
		double second_v = 0; 

		for(String s : q_vect.keySet()) {
			if(q_vect.get(s) > top_v) {
				top = s; 
				top_v = q_vect.get(s);
			}
		}

		for(String s : q_vect.keySet()) {
			if(q_vect.get(s) > second_v && q_vect.get(s) != top_v) {
				second = s; 
				second_v = q_vect.get(s);
			}
		}
		array[0] = top;
		array[1] = second;
		return array; 
	}

	private int findIndex(Double d, ArrayList<Double> values) {
		int index = 0; 
		for(int i = 0; i < values.size(); i++) {
			if(values.get(i) == d)
				index = i; 
		}
		return index; 	
	}

	private boolean alreadyQuery(String term) {
		for(String s : query.split(" "))
			if(s.equals(term)) return true; 
		return false;
	}

	private void addTermsToQVect() {
		for(int i = 0; i < relDoc.size(); i++) {
			for(String s : relDoc.get(i).split(" ")) {
				s = s.toLowerCase().trim().replaceAll("[^\\p{L}\\p{N}]", "");
				if(!s.equals("")) {  //and not one of the stop words 
					if(invListRel.containsKey(s)) {
						int w = invListRel.get(s); 
						invListRel.put(s, w+1);
						q_vect.put(s, 0.0);
					}						
					else {
						q_vect.put(s, 0.0);
						invListRel.put(s, 1);
					}
				}	
			}
		}

		for(int i = 0; i < nonRelDoc.size(); i++) {
			for(String s : nonRelDoc.get(i).split(" ")) {
				s = s.toLowerCase().trim().replaceAll("[^\\p{L}\\p{N}]", "");
				if(!s.equals("")) {  //and not one of the stop words 
					if(invListNonRel.containsKey(s)) {
						int w = invListNonRel.get(s); 
						invListNonRel.put(s, w+1);
						q_vect.put(s, 0.0);
					}						
					else {
						q_vect.put(s, 0.0);
						invListNonRel.put(s, 1);
					}
				}
			}
		}
		System.out.println("");
	}

	private void computeWeightsForOriginalQuery() {
		for(String s : query.split(" ")) {
			s = s.toLowerCase().trim().replaceAll("[^\\p{L}\\p{N}]", "");
			if(!s.equals("")) { //and not one of the stop words
				int w = 0; 
				if(invListRel.containsKey(s))
					w += invListRel.get(s); 			
				if(invListNonRel.containsKey(s))
					w += invListNonRel.get(s);
				if(w != 0)
					q_vect.put(s, Math.log10(10.0 / w));
			}
		}
	}

	private void computeWeightsForQvect() {
		for(String s : q_vect.keySet()) {
			double w = q_vect.get(s); 
			w += BETA / (relDoc.size()) * containsTerm(s, invListRel);
			w += GAMMA / (nonRelDoc.size()) * containsTerm(s, invListNonRel);
			q_vect.put(s, w);	
		}
	}

	private int containsTerm(String term, HashMap<String, Integer> docs) {
		if(docs.containsKey(term)) return docs.get(term);
		else return 0;
	}
}