package com.griddynamics.genesis.test.steps.environments;

import static com.jayway.restassured.RestAssured.get;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.griddynamics.genesis.tools.environments.Environment;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.testng.Assert;

import com.griddynamics.genesis.test.steps.GenesisUtilities;
import com.jayway.restassured.path.json.JsonPath;

import java.util.Collection;

public class EnvironmentBaseSteps extends GenesisUtilities {
    protected int projectId;

    public boolean isEnvironmentExists(String envsListUrl, String envName) {
        return get(envsListUrl).body().asString().contains("\"name\":\"" + envName + "\"");
    }

    public void waitForEnvStatus(Integer envId, long max_time, String expectedStatus) {
        System.out.println("Waiting for env.status is " + expectedStatus);
        String curStatus = "";
        
        long startTime = System.currentTimeMillis();
        long curTime = System.currentTimeMillis();

        try {
            while(curTime - startTime < max_time) {
            	curStatus = getCurrentEnvStatus(envId);

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


    public String getCurrentEnvStatus(Integer id) {
        String json = get(String.format(GET_ENV_DETAILS_URL, projectId, id)).asString();
      
        return (String) JsonPath.from(json).get("status");
    }

    public Integer getEnvironmentId(Integer projectId, final String envName) {
        String json = get(String.format(GET_ENVS_URL, projectId)).asString();
        Collection<Environment> environments = new Gson().fromJson(json, new TypeToken<Collection<Environment>>(){}.getType());
        Environment env = (Environment) CollectionUtils.find(environments, new Predicate() {
            @Override
            public boolean evaluate(Object o) {
                return ((Environment) o).getName().equals(envName);
            }
        });
        return env == null ? null : env.getId();
    }

}
