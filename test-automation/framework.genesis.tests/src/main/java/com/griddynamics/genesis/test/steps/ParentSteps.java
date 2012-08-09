package com.griddynamics.genesis.test.steps;

import static com.jayway.restassured.RestAssured.authentication;
import static com.jayway.restassured.RestAssured.preemptive;

import java.util.Properties;

import junit.framework.Assert;

import org.jbehave.core.annotations.Aliases;
import org.jbehave.core.annotations.BeforeStories;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;

import com.google.gson.Gson;
import com.griddynamics.genesis.tools.CommonResultObjectResponse;
import com.griddynamics.genesis.tools.PropertiesLoader;
import com.griddynamics.genesis.tools.TestRequest;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.parsing.Parser;

/**
 * Class contains common methods and test steps for working with REST API
 * 
 * All other test steps should be inherited from this class
 * 
 * @author mlykosova, ybaturina
 *
 */
public class ParentSteps {
	protected static Boolean isFailedExecution = false;

	protected static Gson gson = new Gson();

	public static TestRequest request = new TestRequest();

	protected static PropertiesLoader propsLoader = new PropertiesLoader();
	
	protected static Properties endpointProperties = propsLoader.loadPropertiesFromFile("endpoints");
	protected static Properties genesisProperties = propsLoader.loadPropertiesFromFile("genesis");

	protected final static String login = genesisProperties.getProperty("login", "genesis");
	protected final static String password = genesisProperties.getProperty("password", "genesis");
	
	protected static String currentUser;
	protected static String currentPass;


	/**
	 * Method contains sanity check if Genesis is accessible.
	 * It's being executed before every test story, if it fails,
	 * then test scenarios are not being executed.
	 */
	@BeforeStories
	public void init() {
		if (isFailedExecution) {
			Assert.assertTrue("Could not execute @BeforeStories", false);
		}

		//Genesis host and port are taken from system properties, 
		//if they are absent there, then from default values in pom.xml
		String host = null;
 		Integer port = null;
		try {
	    	if (System.getProperty("host") != null)  {
	        	host = System.getProperty("host");
	        }
	    	if (System.getProperty("port") != null)  {
	        	port = Integer.parseInt(System.getProperty("port"));
	        }

	    	//Setting REST authorization
	    	setBaseRestAssuredSettings(host, port, login, password);

	    	TestRequest request = new TestRequest("");
	    	request.setIsLogging(false);
	    	request.get();
	    	
		} catch (Exception e) {
			isFailedExecution = true;
			Assert.assertTrue(e.getMessage(), false);
		}
		
		givenILogInSystem();
	}
    
	/**
	 * Method sets REST authorization properties
	 * @param host
	 * @param port
	 * @param user
	 * @param pass
	 */
	
	static void setBaseRestAssuredSettings(String host, Integer port, String user, String pass) {
		RestAssured.baseURI = String.format("http://%s", host);
		RestAssured.port = port;
		RestAssured.defaultParser = Parser.JSON;
        RestAssured.requestContentType("application/json");
        RestAssured.authentication = preemptive().basic(user, pass);
	}

	@Given("I log in system")
	public static void givenILogInSystem() {
		currentUser = login;
		currentPass = password;
		authentication = preemptive().basic(login, password);
	}

	@Given("I log in system with username <userName> and password <password>")
	@Aliases(values = { "I log in system with username $userName and password $password" })
	public static void givenILogInSystemWithCredentials(
			@Named("userName") String user, @Named("password") String pass) {
		currentUser=user;
		currentPass=pass;
		authentication = preemptive().basic(user, pass);
	}
	
	@Then("I expect the request rejected as unathorized")
	public static void thenIExpectUnathorizedRequest() {
		Assert.assertEquals(true, request.checkStatusCode401());
	}
	
	@Then("I expect the request rejected as forbidden")
	public static void thenIExpectForbiddenRequest() {
		Assert.assertEquals(true, request.checkStatusCode403());
	}
	
	@Then("I expect that response with error message <variablesErrors> was returned")
	@Aliases(values = { "I expect that response with error message $variablesErrors was returned" })
	public static void thenIExpectFailedRequestWithError(
			@Named("variablesErrors") String variablesErrors) {
		CommonResultObjectResponse actResponse = request.getResponseObject(CommonResultObjectResponse.class);
		CommonResultObjectResponse expResponse = CommonResultObjectResponse
		.getResultFromString(false, false, "", variablesErrors,
				"", "");
		Assert.assertEquals(expResponse, actResponse);
		Assert.assertEquals(request.checkStatusCode400(), true);
	}
	
	@Then("I expect that response with compound error message <compoundServiceErrors> was returned")
	@Aliases(values = { "I expect that response with compound error message $compoundServiceErrors was returned" })
	public static void thenIExpectFailedRequestWithCompoundError(
			@Named("compoundServiceErrors") String compoundServiceErrors) {
		CommonResultObjectResponse actResponse = request.getResponseObject(CommonResultObjectResponse.class);
		CommonResultObjectResponse expResponse = CommonResultObjectResponse
		.getResultFromString(false, false, "", "",
				compoundServiceErrors, "");
		Assert.assertEquals(expResponse,actResponse);
		Assert.assertEquals(request.checkStatusCode400(), true);
	}
	
	@Then("I expect that response with error message <variablesErrors> and compound error message <compoundServiceErrors> was returned")
	@Aliases(values = { "I expect that response with error message $plainMessage and compound error message $compoundMessage was returned" })
	public static void thenIExpectFailedRequestWithManyErrors(
			@Named("variablesErrors") String variablesErrors, @Named("compoundServiceErrors") String compoundServiceErrors) {
		CommonResultObjectResponse actResponse = request.getResponseObject(CommonResultObjectResponse.class);
		CommonResultObjectResponse expResponse = CommonResultObjectResponse
				.getResultFromString(false, false, "", variablesErrors,
						compoundServiceErrors, "");
		Assert.assertEquals(expResponse, actResponse);
		Assert.assertEquals(request.checkStatusCode400(), true);
	}


    @Given("Running given stories")
    public void RunGivenStories() {
    	System.out.println("");
    }
 }
