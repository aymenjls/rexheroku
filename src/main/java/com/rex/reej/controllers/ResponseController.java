package com.rex.reej.controllers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;


import reactor.core.scheduler.Schedulers;

@Component
@RestController
@RequestMapping("/response")
public class ResponseController {
	private static final Logger LOGGER = LoggerFactory.getLogger(ResponseController.class);

	static String USERNAME;
	@Value("${USERNAME}")
	private String username;
	@Value("${username}")
	private void setUsernameStatic() {
		ResponseController.USERNAME = username;
	}
	static String LOGINURL;
	@Value("${LOGIN_URL}")
	private String loginUrl;
	@Value("${loginUrl}")
	private void setLoginUrlStatic() {
		ResponseController.LOGINURL = loginUrl;
	}
	static String PASSWORD;
	@Value("${PASSWORD}")
	private String password;
	@Value("${password}")
	private void setPasswordStatic() {
		ResponseController.PASSWORD = password;
	}
	static String GRANTSERVICE;
	@Value("${GRANT_SERVICE}")
	private String grantService;
	@Value("${grantService}")
	private void setGrantServiceStatic() {
		ResponseController.GRANTSERVICE = grantService;
	}
	static String CLIENTID;
	@Value("${CLIENT_ID}")
	private String clientId;
	@Value("${clientId}")
	private void setClientIdStatic() {
		ResponseController.CLIENTID = clientId;
	}
	static String CLIENTSECRET;
	@Value("${CLIENT_SECRET}")
	private String clientSecret;
	@Value("${clientSecret}")
	private void setClientSecretStatic() {
		ResponseController.CLIENTSECRET = clientSecret;
	}
	static String RESTENDPOINT;
	@Value("${REST_ENDPOINT}")
	private String restEndpoint;
	@Value("${restEndpoint}")
	private void setRestEndpointStatic() {
		ResponseController.RESTENDPOINT= restEndpoint;
	}

	private static String baseUri;
	private static Header oauthHeader;
	private static Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");

	@PostMapping(value = "/result")
	public HttpResponse getResponse(@RequestBody String responseToPass) throws ClientProtocolException, IOException {
		System.out.println("rrr");
		LOGGER.info("=> GATEWAY : IN RESULT GET MAPPING");
		LOGGER.info(responseToPass);

		HttpClient httpClient = HttpClientBuilder.create().build();
		String loginURL = LOGINURL +
				GRANTSERVICE +
				"&client_id=" + CLIENTID +
				"&client_secret=" + CLIENTSECRET +
				"&username=" + USERNAME +
				"&password=" + PASSWORD;

		HttpPost httpPost = new HttpPost(loginURL);
		HttpResponse response = null;

		try {
			// Execute the login POST request
			LOGGER.info("********* GATEWAY : LOGIN POST");
			response = httpClient.execute(httpPost);
		} catch (ClientProtocolException cpException) {
			cpException.printStackTrace();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}

		// verify response is HTTP OK
		final int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != HttpStatus.SC_OK) {
			LOGGER.info("Error authenticating to Force.com: "+statusCode);
			// Error is in EntityUtils.toString(response.getEntity())
			return null;
		}

		String getResult = null;
		try {
			getResult = EntityUtils.toString(response.getEntity());
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}

		JSONObject jsonObject = null;
		String loginAccessToken = null;
		String loginInstanceLOGIN_URL = null;
		String loginInstanceUrl = null;

		try {
			jsonObject = (JSONObject) new JSONTokener(getResult).nextValue();
			loginAccessToken = jsonObject.getString("access_token");
			loginInstanceUrl = jsonObject.getString("instance_url");
		} catch (JSONException jsonException) {
			jsonException.printStackTrace();
		}

		baseUri = loginInstanceUrl + RESTENDPOINT;
		System.out.println(baseUri);
		oauthHeader = new BasicHeader("Authorization", "OAuth " + loginAccessToken) ;
		LOGGER.info("oauthHeader1: " + oauthHeader);
		LOGGER.info("\n" + response.getStatusLine());
		LOGGER.info("Successful login");
		LOGGER.info("instance URL: "+loginInstanceUrl);
		LOGGER.info("access token/session ID: "+loginAccessToken);
		LOGGER.info("baseUri: "+ baseUri);        


		// release connection
		sendBody(responseToPass);

		httpPost.releaseConnection();
		return null;
	}

	public static void sendBody(String responseToPass) throws ClientProtocolException, IOException {

		String uri = baseUri ;
		System.out.println(baseUri);
		HttpClient httpClient = HttpClientBuilder.create().build();

		HttpPost httpPost = new HttpPost(uri);
		httpPost.addHeader(oauthHeader);
		httpPost.addHeader(prettyPrintHeader);
		StringEntity body;
		try {
			body = new StringEntity(responseToPass);
			httpPost.setEntity(body);
			httpPost.setHeader("Accept", "application/json");
			httpPost.setHeader("Accept-Charset", "utf-8");
			httpPost.setHeader("Content-type", "application/json");
			LOGGER.info("body1: "+ responseToPass);
			httpClient.execute(httpPost);
			
		} catch (ClientProtocolException cpException) {
			cpException.printStackTrace();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}

	}

	public static void logErrorBody(ClientResponse response) {
		if (LOGGER.isErrorEnabled()) {
			response.bodyToMono(String.class)
			.publishOn(Schedulers.boundedElastic())
			.subscribe(body -> LOGGER.error("Body of the #Salesforce error response: {}", body));
		}
	}
}
