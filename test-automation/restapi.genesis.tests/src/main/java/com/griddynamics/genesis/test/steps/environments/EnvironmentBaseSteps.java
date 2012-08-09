package com.griddynamics.genesis.test.steps.environments;

import static com.jayway.restassured.RestAssured.get;

import org.testng.Assert;

import com.griddynamics.genesis.test.steps.GenesisUtilities;
import com.jayway.restassured.path.json.JsonPath;

public class EnvironmentBaseSteps extends GenesisUtilities {
    protected int projectId;

    public boolean isEnvironmentExists(String envsListUrl, String envName) {

        return get(envsListUrl).body().asString().contains("\"name\":\"" + envName + "\"");

    }

    public void waitForEnvStatus(String envName, long max_time, String expectedStatus) {
        System.out.println("Waiting for env.status is " + expectedStatus);
        String curStatus = "";
        
        long startTime = System.currentTimeMillis();
        long curTime = System.currentTimeMillis();

        try {
            while(curTime - startTime < max_time) {
            	curStatus = getCurrentEnvStatus(envName);

                System.out.println(curStatus);

                if (curStatus.contains("Busy")) {
                    Thread.sleep(30000);
                    curTime = System.currentTimeMillis();
                } else {
                    break;
                }
            }

            Assert.assertTrue(curStatus.equals(expectedStatus), "[Error] Invalid summary env.status = " + curStatus + ". Expected status is " + expectedStatus);

        } catch (Exception e) {
            Assert.assertTrue(false, "Exception into waitForReadyStatus() " + e.getMessage());
        }

    }


    public String getCurrentEnvStatus(String envName) {
        String json = get(String.format(GET_ENV_DETAILS_URL, projectId, envName)).asString();
      
        return (String) JsonPath.from(json).get("status");
    }


}
