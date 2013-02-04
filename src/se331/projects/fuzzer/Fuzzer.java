package se331.projects.fuzzer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;

public class Fuzzer {
	
	private static ArrayList<String> urlsVisited;
	private static final String baseURL = "http://localhost:8080/bodgeit/";
	private static HashMap<String, List<String>> urlParameterMap = new HashMap<String, List<String>>();
	private static Set<Cookie> cookiesSet = new HashSet<Cookie>();

	public static void main(String[] args) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		WebClient webClient = new WebClient();
		webClient.setJavaScriptEnabled(true);
		//discoverLinks(webClient);
		//doFormPost(webClient);
		//printCookies(webClient.getCookieManager());
		printLinkDiscovery(webClient);
		printPageGuessing(webClient, "conf/GuessURLs.txt");
		printURLMap();
		webClient.closeAllWindows();
	}

	/**
	 * This code is for showing how you can get all the links on a given page, and visit a given URL
	 * @param webClient
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	/*
	private static void discoverLinks(WebClient webClient) throws IOException, MalformedURLException {
		HtmlPage page = webClient.getPage(baseURL);
		List<HtmlAnchor> links = page.getAnchors();
		for (HtmlAnchor link : links) {
			System.out.println("Link discovered: " + link.asText() + " @URL=" + link.getHrefAttribute());
		}
		
		printFormInputs(page);
	}*/

	/**
	 * This code is for demonstrating techniques for submitting an HTML form. Fuzzer code would need to be
	 * more generalized
	 * @param webClient
	 * @throws FailingHttpStatusCodeException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	/*
	private static void doFormPost(WebClient webClient) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		HtmlPage page = webClient.getPage("http://localhost:8080/bodgeit/product.jsp?prodid=26");
		List<HtmlForm> forms = page.getForms();
		for (HtmlForm form : forms) {
			HtmlInput input = form.getInputByName("quantity");
			input.setValueAttribute("2");
			HtmlSubmitInput submit = (HtmlSubmitInput) form.getFirstByXPath("//input[@id='submit']");
			System.out.println(submit.<HtmlPage> click().getWebResponse().getContentAsString());
		}
		
		printFormInputs(page);
	}*/
	
	/**
	 * This prints out all the cookies contained in the CookieManager.
	 * Call this after visiting a page to get the cookies that were set.
	 * @author jmd2188
	 * @param cookieManager	CookieManager instance to retrieve cookies from
	 */
	private static void printCookies( CookieManager cookieManager ) {
		Set<Cookie> cookies = cookieManager.getCookies();
		System.out.println(String.format("%30s", "").replace(' ', '*'));
		for ( Cookie cookie : cookies ) {
			if ( !cookiesSet.contains(cookie) ) {
				System.out.println("Newly Set Cookies:");
				cookiesSet.add(cookie);
				String key = cookie.getName();
				String val = cookie.getValue();
				String domain = cookie.getDomain();
				String path = cookie.getPath();
				Date expires = cookie.getExpires();
				String expiresAt = expires == null ? "Never" : DateFormat.getDateInstance().format( expires );
				System.out.println(String.format("%-20s = %-32s", key, val));
				System.out.println(String.format("....for %s on %s, expires %s", path, domain, expiresAt));
			}
		}
		System.out.println(String.format("%30s", "").replace(' ', '*'));
	}
	
	/**
	 * Prints out all form inputs on a page. Just finds all <input> elements and
	 *  prints their name and value attributes.
	 * @author jmd2188
	 * @param page HtmlPage object to search for inputs
	 */
	private static void printFormInputs( HtmlPage page ) {
		System.out.println(String.format("%30s", "").replace(' ', '*'));
		DomNodeList<HtmlElement> inputs = page.getElementsByTagName("input");
		if ( inputs.isEmpty() ) {
			System.out.println(String.format("No form inputs on %s", page.getUrl().toString()));
		} else {
			System.out.println( String.format("Form inputs on %s:", page.getUrl().toString() ));
			System.out.println(String.format("%-10s %-10s %-10s", "Name", "Type", "Default"));
			/*
			List<HtmlForm> forms = page.getForms();
			for (HtmlForm f : forms ) {
				for ( HtmlElement kind : f.getChildElements() ) {
					if ( kind instanceof HtmlInput ) {
						HtmlInput input = (HtmlInput)kind;
						String output = String.format("%-10s %-10s %-10s", input.getNameAttribute(), input.getTypeAttribute(), input.getDefaultValue());
						System.out.println(output);
					}
				}
			}
	 		*/
			for ( HtmlElement el : inputs ) {
				HtmlInput input = (HtmlInput)el;
				String output = String.format("%-10s %-10s %-10s", input.getNameAttribute(), input.getTypeAttribute(), input.getDefaultValue());
				System.out.println(output);
			}
		}
		System.out.println(String.format("%30s\n", "").replace(' ', '*'));
	}
	
	/**
	 * Prints all of the links that the crawl reveals.  Does not visit external sites
	 * @author alh9634
	 * @param webClient the WebClient instance to use
	 */
	private static void printLinkDiscovery( WebClient webClient){
		System.out.println("\n\n\n\nBegin Link Discovery");
		urlsVisited = new ArrayList<String>();
		printLinkDiscovery_helper(webClient, "");
	}

	/**
	 * Helper used to crawl through all of the web pages
	 * 
	 * @author alh9634
	 * @param webClient the WebClient instance to use
	 * @param hrefAttribute the href attribute to visit
	 */
	private static void printLinkDiscovery_helper(WebClient webClient, String hrefAttribute) {
		System.out.println("Link discovered through crawl: " + hrefAttribute);
		urlsVisited.add(hrefAttribute);
		printCookies(webClient.getCookieManager());
		ParseURL(hrefAttribute);
		HtmlPage page;
		try {
			page = webClient.getPage(baseURL + hrefAttribute);
			printFormInputs(page);
		} catch (Exception e) {
			System.out.println("External/Invalid URL: " + hrefAttribute);
			return;
		}
		List<HtmlAnchor> links = page.getAnchors();
		for (HtmlAnchor link : links) {
			if (!urlsVisited.contains(link.getHrefAttribute())) {
				printLinkDiscovery_helper(webClient, link.getHrefAttribute());
			}
		}
	}

	/**
	 * Goes through the guess URL file and determines which are actually valid
	 * 
	 * @author alh9634
	 * @param webClient the WebClient instance to use
	 * @param guessURLsLocation file location that contains all of the guess URLs to check
	 */
	private static void printPageGuessing(WebClient webClient, String guessURLsLocation) {
		ArrayList<String> guessURLs = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(guessURLsLocation));
			String line = br.readLine();

			while (line != null) {
				guessURLs.add(line);
				line = br.readLine();
			}
			br.close();
		} catch (Exception e) {
			System.out.println(e);
		}
		
		for(String guess : guessURLs){
			try {
				webClient.getPage(baseURL + guess);
				System.out.println("Successful guess: " + guess);
			} catch (Exception e){
				System.out.println("The following guess did not work: " + guess);
			}
		}
	}
	
	/**
	 * Adds the specific href into the URL Parameter Map
	 * 
	 * @author alh9634
	 * @param hrefAttribute
	 */
	private static void ParseURL(String hrefAttribute){
		if(hrefAttribute.indexOf('?')>=0){
			String key = hrefAttribute.substring(0, hrefAttribute.indexOf("?"));
			if(!urlParameterMap.keySet().contains(key)){
				urlParameterMap.put(key, new ArrayList<String>());
			}
			String paramString = hrefAttribute.substring(hrefAttribute.indexOf('?') + 1);
			String[] params = paramString.split("&");
			for(String param : params){
				String stripValue = param.substring(0, param.indexOf('='));
				if(!urlParameterMap.get(key).contains(stripValue)){
					urlParameterMap.get(key).add(stripValue);
				}
			}
		}else{
			if(!urlParameterMap.keySet().contains(hrefAttribute)){
				urlParameterMap.put(hrefAttribute, new ArrayList<String>());
			}
		}
	}
	
	/**
	 * Prints the URL Parameter Map
	 * 
	 *@author alh9634
	 */
	private static void printURLMap(){
		System.out.println("URL Parameter Map: ");
		Set<String> keys = urlParameterMap.keySet();
		for(String key : keys){
			if ( !urlParameterMap.get(key).isEmpty() ) {
				System.out.println("\tURL: " + key);
				System.out.println("\tParameters: " + urlParameterMap.get(key));
			}
		}
	}
}
