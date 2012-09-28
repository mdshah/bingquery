package com.servlet.explore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.servlet.explore.JSONArray;  
import java.io.PrintWriter;
import com.servlet.explore.JSONObject;  
import org.apache.commons.codec.binary.Base64;


/**
 * Servlet implementation class MovieServlet
 */
@WebServlet("/Search")
public class Search extends HttpServlet {
	private static final long serialVersionUID = 1L;

    /**
     * Default constructor. 
     */
    public Search() {
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		try{
			
			        String navBar;
		        
		            navBar = "<p>Welcome!</p>";
		        
		        resp.setContentType("text/html");
		        PrintWriter out = resp.getWriter();
		        out.println(navBar);
			
			resp.setContentType("text/html");
			
			String data="";
			data=data+"<html>";
			data=data+"<body>";
			data=data+"<head>";
			data=data+"<form method=\"post\"ACTION=\"/search/Search\">  ";
			data=data+"Search Bing: <input type=\"text\" name=\"query\" /><br />";
			data=data+"</form>";
			data=data+"</head>";
			data=data+"</html>";
			resp.getWriter().println(data); 
			
			
				  }  
			   catch (Exception te) {
	            te.printStackTrace(resp.getWriter());
	             //System.exit(-1);
	        }
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		try{
			
			 String query=req.getParameter("query");
			 System.out.println(query);
			String accountKey = "PJD4UwjOC50tzY6BN95L7ftRuQ5EMavSK14aCsiiEjc=";
			String bingUrl = "https://api.datamarket.azure.com/Data.ashx/Bing/SearchWeb/v1/Web?Query=%27"+query+"%27&$top=10&$format=JSON";
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
//		resp.getWriter().println(json);
		
JSONObject jo = new JSONObject(json);

JSONArray ja;
jo = jo.getJSONObject("d");
ja = jo.getJSONArray("results");

int size = ja.length();
for (int i = 0; i < size; i++)
{
    JSONObject resultObject = ja.getJSONObject(i);
    resp.getWriter().println(resultObject.get("Title"));
    resp.getWriter().println(resultObject.get("Description"));
    resp.getWriter().println(resultObject.get("Url"));
    resp.getWriter().println("--");
}


		}
		 catch (Exception te) {
	            te.printStackTrace(resp.getWriter());
	             //System.exit(-1);
	        }
	        
	}

}
