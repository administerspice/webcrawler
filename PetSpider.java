import java.util.*;
import java.io.*;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class PetSpider
{
  private static Queue<URL> frontier = new LinkedList<URL>();
  private static HashMap<URL, Boolean> sitelist = new LinkedHashMap<URL, Boolean>();
  private static LinkedList<URL> disallowlist = new LinkedList<URL>();
  private static URL seed;
  private static int linkcount;
  
  public static void main(String[] args) throws IOException, InterruptedException
  { 
    // System.print to out file
//    try{
////      PrintStream out = new PrintStream(new FileOutputStream("urls.txt"));
////    	PrintStream out = new PrintStream(new FileOutputStream("data1000.txt"));
//	  	PrintStream out = new PrintStream(new FileOutputStream("urls1000.txt"));
//      System.setOut(out);
//    }catch (Exception ex) {
//      ex.printStackTrace();
//    }	
	  
	  // Set seed URL
	  try {
		  seed = new URL("http://ciir.cs.umass.edu");
	  } catch (MalformedURLException ex) {
		  System.out.println("MalformedURLException in main");
		  ex.printStackTrace();
	  }
	  
	  crawlA(seed);	// Part A: get 100 links, limited to "cs.umass.edu"
//	  crawlB(seed);	// Part B: get 1000 links, no domain limit
  }
  
  // Checks of robots.txt page exists
  public static Boolean hasRobots(URL website) throws IOException {	// Check if robots.txt exists
	URL botURL;
	String strURL = website.toString();
	try {
		if(strURL.substring(strURL.length()-1) == "/"){
			botURL = new URL(website + "robots.txt");
		}else{
			botURL = new URL(website + "/robots.txt");
		}
		if(!isGoodLink(botURL)) return false; 
	} catch (MalformedURLException e1) {
		System.out.println("MalformedURLException in hasRobots()");
		e1.printStackTrace();
	}
    return true;
  }
  
  // Checks if the page exists
  public static Boolean isGoodLink(URL website) throws IOException {
  	  try {
  		  HttpURLConnection http = (HttpURLConnection)website.openConnection();
  		  int statusCode = http.getResponseCode();
  		  if(statusCode != 200) return false;
  	  } catch (MalformedURLException ex) {
  		  ex.printStackTrace();
  	  }
	  return true;
  }
   
  // Updates robot rules list and checks it for current site
  public static Boolean isAllowed(URL website) throws IOException, InterruptedException{	// Allowed to search and index this site?FIX
	  parseRobots(website);
	  if(disallowlist.contains(website)) return false;
	  return true;
  }
  
  // Robot parser: Handles disallows
  public static void parseRobots(URL website) throws MalformedURLException, IOException, InterruptedException{
	  List<String> lines = new LinkedList<String>();
	  String strURL;
	  URL botURL;
	  if(hasRobots(website)){
		  strURL = website.toString();
		  if(strURL.substring(strURL.length()-1) == "/"){
				botURL = new URL(website + "robots.txt");
		  }else{
				botURL = new URL(website + "/robots.txt");
		  }
		  
		// Read file by line and make list of lines
		  BufferedReader in = new BufferedReader(new InputStreamReader(botURL.openStream()));
		  String str;
		  while ((str = in.readLine()) != null) {
		   	lines.add(str);
		  }
		  in.close();
		  
		  // Finds relevant lines in robots.txt lines
		  for(String line : lines){
			// If found applies to all bots line, go to next line 
		  	if(line.contains("User-agent: *")) continue;
		  	// If Disallow line
		   	if(line.contains("Disallow: /")){
		   		// Extract/add disallow path
		   		String path = line.substring(line.indexOf("/"));
		   		URL newRule = new URL(website+path);
		   		// Check if URL or path is in list. Add if not
		   		if(!disallowlist.contains(newRule)) 
		   			disallowlist.add(newRule);
		   	}
		   	// Handle crawl delay
		   	if(line.contains("Crawl-delay:")){
		   		// Extract delay time
		   		String time = line.substring(line.indexOf(" ")+1);
		   		long delay = 1000*Integer.parseInt(time);
		   		Thread.sleep(delay);
		   	}
		   	// Add Allow paths to frontier queue
		   	if(line.contains("Allow: /")){
		   		String allow = line.substring(line.indexOf("/"));
		   		URL newlink = new URL(website+allow);
		   		frontier.add(newlink);
		   		if(isUnique(newlink)){
		   			linkcount++;
		   			sitelist.put(newlink, false);
		   		}
		   	}
		   	if(line.contains("User-agent")) break;
		  }
	  }  
  }
  
  public static Boolean isUnique(URL website){
	  if(sitelist.containsKey(website)) return false;
	  return true;
  }

  // Crawler method for Part A
  public static void crawlA(URL seed) throws IOException, InterruptedException{
	  URL del;
	  linkcount = 0;
	  frontier.add(seed);  // Add seed to frontier
	  URL website = seed;
	  while( (website = frontier.peek()) != null && linkcount < 100)
	  {	
		  // Regular Expression to match cs.umass.edu
		  Pattern csumass = Pattern.compile("https?://(.*\\.)?cs\\.umass\\.edu[^#]*");
		  Matcher matcher = csumass.matcher(website.toString());
		  if(!matcher.matches()){
			  del = frontier.remove();
			  continue;
		  }
	  	  if(!isUnique(website)) {
	  		  del = frontier.remove();
	  		  continue;
	  	  }
	   	// Check if URL connects
	   	if(!isGoodLink(website)) {
	   		del = frontier.remove();
	   		continue;
	   	}	  
	   	// Respect robots.txt
	   	if(!isAllowed(website)){
	   		del = frontier.remove();
	   		// Do not search but create hash entry with FALSE
	   		sitelist.put(del, false);
	   		linkcount++;
	   		continue;
	   	}
	   	// Get HTML and its links
	   	try{
	   		String url = website.toString();
	   		Document html = Jsoup.connect(url).get();
	   		Elements links = html.select("a[href]");	// Creates list of href links from HTML
	   		if(!links.isEmpty()){
		   		for (Element link : links){
		   			URL n = new URL(link.attr("abs:href"));
		   			frontier.add(n);
		   		}
	    	}
	    }catch(MalformedURLException ex){
	    	del = frontier.remove();
	    	continue;
	    }catch(Exception ex){
	    	del = frontier.remove();
	    	continue;
	    }
//	 	Add current website to master list and mark as searched
	    sitelist.put(website, true);
	    linkcount++;
	  } // End while loop

//	     Print list of 100 unique URLs
	    Set<URL> keys = sitelist.keySet();
	    for(URL key : keys){
	    	System.out.println(key);
	    }
	    
	//*************************END PART A CRAWLER************************//
  }
  
  // Crawler method for Part B
  public static void crawlB(URL seed) throws IOException, InterruptedException{
	  linkcount = 0;
	  int pagesSearched = 0;
	  frontier.add(seed);  // Add seed to frontier
	  URL website = seed;
	  while( (website = frontier.peek()) != null && linkcount < 1000)
	  {
		  URL del;
		  Pattern csumass = Pattern.compile("https?://.*[^#]");
		  Matcher matcher = csumass.matcher(website.toString());
		  // Check if matching url
		  if(!matcher.matches()) {
			  del = frontier.remove();
			  continue;
		  }
		  // Does the page exist?
		  if(!isGoodLink(website)){	
			  del = frontier.remove();
			  continue;
		  }
		  // Check disallowlist
		  if(!isAllowed(website)){
			  URL temp = frontier.remove();
			  // Do not search but create hash entry with FALSE
	    		if(isUnique(website)){
	    			sitelist.put(temp, false);
	    			linkcount++;
	    		}
	    		continue;
	    	}
	    	// Get HTML and its links
	    	try{
	    		String url = website.toString();
	    		Document html = Jsoup.connect(url).get();
	    		Elements links = html.select("a[href]");
	    		if(!links.isEmpty()){
		    		for (Element link : links){
		    			String s = link.attr("abs:href");
		    			URL n = new URL(s);
		    			if(isUnique(n)){
		    				frontier.add(n);
		    				sitelist.put(n, false);
		    				linkcount++;
		    			}
		    		}
	    		}
	    	}catch(MalformedURLException ex){
	    		frontier.remove();
	    		continue;
	    	}catch(Exception ex){
	    		frontier.remove();
	    		continue;
	    	}
	    	// 	Add current website to master list and mark as searched
    		frontier.remove();
	    	if(isUnique(website)){
	    		sitelist.put(website, true);
	    		linkcount++;
	    	}
	    	pagesSearched++;
	    	// Print # unique links and # documents searched at each step
//	    	System.out.println("Pages Searched: " + pagesSearched + "\tLink Count: " + linkcount);
	    } // End while loop
//	     Print list of 1000 unique URLs
	    Set<URL> keys = sitelist.keySet();
	    for(URL key : keys){
	    	System.out.println(key);
	    }
	//*************************END PART B CRAWLER************************// 
  }
}