package se331.projects.fuzzer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.gargoylesoftware.htmlunit.util.UrlUtils;

public class Fuzzer {
	
	private static ArrayList<URL> urlsVisited;
	//private static final String baseURL = "http://127.0.0.1:8080/jpetstore/";
	private static final String baseURL = "http://127.0.0.1/dvwa/";
	private static HashMap<String, List<String>> urlParameterMap = new HashMap<String, List<String>>();
	private static Set<Cookie> cookiesSet = new HashSet<Cookie>();
	private static String username = "";
	private static String password = "";

	public static void main(String[] args) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		WebClient webClient = new WebClient();
		webClient.setPrintContentOnFailingStatusCode(false);
		webClient.setJavaScriptEnabled(true);
		
		username = "admin";
		password = "password";
		
		printLinkDiscovery(webClient);
		
		System.out.println("\n\n\nBeginning Page Guessing");
		printPageGuessing(webClient, "conf/GuessURLs.txt");
		System.out.println("\n\n");
		printCookies(webClient.getCookieManager());
		System.out.println("\n\nURL Map:");
		printURLMap();
		webClient.closeAllWindows();
	}
	
	/**
	 * This prints out all the cookies contained in the CookieManager.
	 * Call this after visiting a page to get the cookies that were set.
	 * @author jmd2188
	 * @param cookieManager	CookieManager instance to retrieve cookies from
	 */
	private static void printCookies( CookieManager cookieManager ) {
		Set<Cookie> cookies = cookieManager.getCookies();
		System.out.println(String.format("%30s", "").replace(' ', '*'));
		System.out.println("Cookies:");
		for ( Cookie cookie : cookies ) {
			if ( !cookiesSet.contains(cookie) ) {
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
		urlsVisited = new ArrayList<URL>();
		URL myURL = null;
		try {
			myURL = new URL(baseURL);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		printLinkDiscovery_helper(webClient, myURL);
	}

	/**
	 * Helper used to crawl through all of the web pages
	 * 
	 * @author alh9634
	 * @param webClient the WebClient instance to use
	 * @param myURL the URL visited
	 */
	private static void printLinkDiscovery_helper(WebClient webClient, URL myURL) {
		if(myURL == null){
			return;
		}
		System.out.println("Link discovered through crawl: " + myURL);
		urlsVisited.add(myURL);
		ParseURL(myURL);
		HtmlPage page;
		try {
			page = webClient.getPage(myURL);
			printFormInputs(page);
		} catch (Exception e) {
			System.out.println("External/Invalid URL: " + myURL);
			return;
		}
		List<HtmlAnchor> links = page.getAnchors();
		for (HtmlAnchor link : links) {
			URL tempURL = null;
			try {
				tempURL = page.getFullyQualifiedUrl(link.getHrefAttribute());
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return;
			}
			if (!urlsVisited.contains(tempURL) && tempURL.getHost().equals(myURL.getHost())) {
				printLinkDiscovery_helper(webClient, tempURL);
			}
		}
		
		if ( isLoginPage(page) ) {
			URL loggedInURL = doLogin( webClient, page, username, password );
			if (loggedInURL != null && !urlsVisited.contains(loggedInURL) ) {
				printLinkDiscovery_helper(webClient, loggedInURL);
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
	 * Adds the specific url into the URL Parameter Map
	 * 
	 * @author alh9634
	 * @param myURL the URL to parse
	 */
	private static void ParseURL(URL myURL){
		if(!urlParameterMap.keySet().contains(myURL.getPath())){
			urlParameterMap.put(myURL.getPath(), new ArrayList<String>());
		}
		
		if(myURL.getQuery()==null){
			return;
		}
		String[] params = myURL.getQuery().split("&");
		for(String param : params){
			String stripValue = param.substring(0, param.indexOf('='));
			if(!urlParameterMap.get(myURL.getPath()).contains(stripValue)){
				urlParameterMap.get(myURL.getPath()).add(stripValue);
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
			System.out.println("\tURL: " + key);
			System.out.println("\tParameters: " + urlParameterMap.get(key));
		}
	}
	
	/**
	 * Perform login with given credentials
	 * @author jmd2188
	 * 
	 * @param client WebClient to perform requests with
	 * @param loginPage page with login form
	 * @param user username to use
	 * @param pw password to use
	 * @return post-login url
	 */
	private static URL doLogin(WebClient client, HtmlPage loginPage, String user, String pw) {
		String userField = "";
		String pwField = "";
		String method = "";
		String loginBtn = "";
		HtmlForm loginForm = null;
		URL nextUrl = null;
		URL targetUrl = null;
		try {
			List<HtmlElement> inputs = loginPage.getElementsByTagName("input");
			for ( HtmlElement el : inputs ) {
				HtmlInput i = (HtmlInput)el;
				String type = i.getTypeAttribute();
				
				if ( type.equals("text") && i.getNameAttribute().matches(".*user.*") && userField.isEmpty() ) {
					userField = i.getNameAttribute();
					System.out.println("User Field: " + userField);
				} else if ( type.matches(".*pass.*|.*pw.*") && pwField.isEmpty() ) {
					pwField = i.getNameAttribute();
					System.out.println("Password Field: " + pwField);
					loginForm = i.getEnclosingFormOrDie();
					
					if ( targetUrl == null ) {
						targetUrl = new URL(UrlUtils.resolveUrl(baseURL, loginForm.getActionAttribute()));
						System.out.println("Login Target: " + targetUrl.toString());
					}
					if ( method.isEmpty() ) {
						method = loginForm.getMethodAttribute().toUpperCase();
					}
				} else if ( type.equals("submit") && loginBtn.isEmpty() ) {
					loginBtn = i.getNameAttribute();
				}
			}
			
			if ( !pwField.isEmpty() && ! userField.isEmpty() && targetUrl != null ) {
				System.out.println(String.format("Attempting to login to '%s' with username '%s' and password '%s'", targetUrl, user, pw));
				
				loginForm.getInputByName(userField).setValueAttribute(user);
				loginForm.getInputByName(pwField).setValueAttribute(pw);
				HtmlPage loggedInPage = loginForm.getInputByName(loginBtn).click();
				
				nextUrl = loggedInPage.getUrl();
				System.out.println("Post-login URL: " + nextUrl.toString());
			}
		} catch (FailingHttpStatusCodeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return nextUrl;
	}
	
	/**
	 * Does this page have a login form. Just look for an input of type 'password'
	 * @author jmd2188
	 * 
	 * @param page HtmlPage to check for login form
	 * @return true if login form exists; otherwise false
	 */
	private static boolean isLoginPage( HtmlPage page ) {
		List<HtmlElement> inputs = page.getElementsByTagName("input");
		for ( HtmlElement el : inputs ) {
			if ( el.getAttribute("type").equalsIgnoreCase("password") ) {
				return true;
			}
		}
		return false;
	}
}
