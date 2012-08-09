package com.griddynamics.genesis.tools;


import static com.jayway.restassured.RestAssured.*;

import com.jayway.restassured.mapper.ObjectMapper;
import com.jayway.restassured.response.Response;


public class TestRequest {
	private static final int HTTP_OK = 200;
	private static final int HTTP_BAD_REQUEST = 400;
	private static final int HTTP_UNATHORIZED_REQUEST = 401;
	private static final int HTTP_FORBIDDEN_REQUEST = 403;

	private String url;
    private Boolean isLogging = true;
    private Response response;

    public TestRequest() {
		super();
	}

	public TestRequest(String url) {
        this.url = url;
    }

    public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
		System.out.println(url);
	}

	public Boolean getIsLogging() {
		return isLogging;
	}

	public void setIsLogging(Boolean isLogging) {
		this.isLogging = isLogging;
	}

    public Response getResponse() {
		return response;
	}

	public void setResponse(Response response) {
		this.response = response;
	}

	public void get() {
		response = isLogging ? given().log().body().expect().log().body().get(url) : with().get(url);
	}

	public void post(String body) {
		response = isLogging ? given().log().body().body(body).expect().log().body().when().post(url) : with().body(body).post(url);
	}

	public void post(Object object) {
		response = isLogging ? given().log().body().body(object, ObjectMapper.GSON).expect().log().body().when().post(url) : with().body(object, ObjectMapper.GSON).post(url);
	}

	public void delete() {
		response = isLogging ? given().expect().log().body().when().delete(url) : with().delete(url);
	}

	public void  put(String body) {
		response = isLogging ? given().log().body().body(body).expect().log().body().put(url) : with().body(body).put(url);
	}

	public void put(Object object) {
		response = isLogging ? given().log().body().body(object, ObjectMapper.GSON).expect().log().body().put(url) : with().body(object, ObjectMapper.GSON).put(url);
	}

	public boolean checkStatusCode200() {
		return response.getStatusCode() == HTTP_OK ? true : false;
	}
	
	public boolean checkStatusCode400() {
		return response.getStatusCode() == HTTP_BAD_REQUEST ? true : false;
	}
	
	public boolean checkStatusCode401() {
		return response.getStatusCode() == HTTP_UNATHORIZED_REQUEST ? true : false;
	}
	
	public boolean checkStatusCode403() {
		return response.getStatusCode() == HTTP_FORBIDDEN_REQUEST ? true : false;
	}

	public <T> T getResponseObject(Class <T> cls) {
		return getResponse().as(cls, ObjectMapper.GSON);
	}

}
