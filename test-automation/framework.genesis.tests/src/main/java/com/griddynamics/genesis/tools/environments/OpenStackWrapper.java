package com.griddynamics.genesis.tools.environments;

import com.jayway.restassured.RestAssured;
import static com.jayway.restassured.RestAssured.*;

public class OpenStackWrapper {

    String OPEN_STACK_API_URL = "";
    int OPEN_STACK_API_PORT = 8774;
    String TOKEN_HEADER = "X-Auth-Token";
    String LOGIN_HEADER_NAME = "X-Auth-User";
    String LOGIN = "";
    String AUTH_KEY_HEADER_NAME = "X-Auth-Key";
    String AUTH_KEY = "";
    String API_URL = "";
    String GET_SERVER_LIST_URL = "/servers";

    String token;

    void init() {
        RestAssured.reset();

        RestAssured.baseURI = OPEN_STACK_API_URL;
        RestAssured.port = OPEN_STACK_API_PORT;
        setToken();
    }


    void setToken() {

        token = given().
                       headers(LOGIN_HEADER_NAME, LOGIN, AUTH_KEY_HEADER_NAME, AUTH_KEY).
                when().
                       get("/v1.1").getHeader(TOKEN_HEADER);

    }

    String getToken() {

        return token;

    }

}
