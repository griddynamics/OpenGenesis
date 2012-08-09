package com.griddynamics.genesis.test.steps;

import static com.jayway.restassured.RestAssured.preemptive;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;

import junit.framework.Assert;

import com.griddynamics.genesis.test.steps.ParentSteps;
import com.griddynamics.genesis.tools.TestRequest;
import com.griddynamics.genesis.tools.credentials.Credentials;
import com.griddynamics.genesis.tools.projects.ProjectDetails;
import com.griddynamics.genesis.tools.servers.ServerArray;
import com.jayway.restassured.RestAssured;


public class GenesisUtilities extends ParentSteps {

	protected static String GET_PROGECTS_URL  = endpointProperties.getProperty("projects.path");
	protected static String GET_CREDENTIALS_URL  = endpointProperties.getProperty("credentials.list");
	protected static String GET_SERVERS_ARRAYS_URL = endpointProperties.getProperty("server.arrays.get");
	protected static String DELETE_SERVER_ARRAY_URL  = endpointProperties.getProperty("server.array.delete");
	protected static String CREATE_SERVER_ARRAY_URL = endpointProperties.getProperty("server.array.create");
	protected static String ADD_CREDENTIALS_URL = endpointProperties.getProperty("credentials.add");
	protected static String DELETE_CREDENTIALS_URL  = endpointProperties.getProperty("credentials.delete");
	protected static String CREATE_ENV_URL = endpointProperties.getProperty("env.create");
	protected static String GET_ENVS_URL = endpointProperties.getProperty("envs.get");
	protected static String GET_ENV_DETAILS_URL = endpointProperties.getProperty("env.details");
	protected static String DELETE_ENV_URL = endpointProperties.getProperty("env.delete");
	protected static String GET_SERVERS_ARRAY_LIST_URL = endpointProperties.getProperty("server.arrays.get");
	protected static String UPDATE_SETTINGS_URL = endpointProperties.getProperty("settings.update");
	protected static String GET_SETTINGS_URL = endpointProperties.getProperty("settings.list");
	protected static String RESET_SETTINGS_URL  = endpointProperties.getProperty("settings.reset");
	protected static String GET_TEMPLATES_CONTENT_URL  = endpointProperties.getProperty("template.content");
	protected static String GET_TEMPLATES_DESCRIPTION_URL  = endpointProperties.getProperty("template.description");
	
	
	public static int getProjectIdByProjectName(String projectName) {
		RestAssured.authentication = preemptive().basic(login, password);
		TestRequest request = new TestRequest(GET_PROGECTS_URL);
		request.get();
		RestAssured.authentication = preemptive().basic(currentUser,
				currentPass);
		ProjectDetails[] projects = request
				.getResponseObject(ProjectDetails[].class);

		for (int i = 0; i < projects.length; i++) {
			if (projects[i].getName().equals(projectName)) {
				return projects[i].getId();
			} else {
				continue;
			}
		}

		return -1;
	    }
	    
	    public int getCredentialId(String projectId, String pairName, String cloudProvider, String identity) {
		    TestRequest request = new TestRequest(String.format(GET_CREDENTIALS_URL, projectId));
	    	request.get();

	    	Credentials[] credentials = request.getResponseObject(Credentials[].class);
	    	
	        for (int i = 0; i < credentials.length; i++) {
	            if (credentials[i].getPairName().equals(pairName) &&
	            		credentials[i].getCloudProvider().equals(cloudProvider) &&
	            		credentials[i].getIdentity().equals(identity)) {
	            	return credentials[i].getId();
	            } else {
	            	continue;
	            }
	        }

	    	return -1;
	    }
	    
	    public boolean findCredentialsById(int id, String projectId) {
		    TestRequest request = new TestRequest(String.format(GET_CREDENTIALS_URL, projectId));
	    	request.get();

	    	Credentials[] credentials = request.getResponseObject(Credentials[].class);
	    	
	        for (int i = 0; i < credentials.length; i++) {
	            if (credentials[i].getId() == id) {
	            	return true;
	            } else {
	            	continue;
	            }
	        }

	    	return false;

	    }
	    
	    public int getServerArrayId(int projectId, String serverArrayName) {
		    TestRequest request = new TestRequest(String.format(GET_SERVERS_ARRAYS_URL, projectId));
	    	request.get();
	    	
            ServerArray[] arrays = request.getResponseObject(ServerArray[].class);

	        for (int i = 0; i < arrays.length; i++) {
	            if (arrays[i].getName().equals(serverArrayName)) {
	            	return arrays[i].getId();
	            } else {
	            	continue;
	            }
	        }

	    	return -1;
	    }

	    protected String getFileContentFromResourses(String fileName) throws IOException {
	    	InputStream is =getClass().getResourceAsStream("/" + fileName);

	        InputStreamReader isr = new InputStreamReader(is);
	        BufferedReader br = new BufferedReader(isr);

	        String content = "";
	        String str = br.readLine();

	        while(str != null) {
	        	content = content + str + "\n";
	        	str = br.readLine();
	        }

	    	return content.trim();
	    }

	    protected String getKeyFromFile(String path, String fileName)  {
	        String str;
	        String key = "";
	        try {
	        	File file = new File(path + fileName);
	        	
	        	Assert.assertTrue("File is not found [" + file.getPath() + "]", file.exists());
	        	
	            if (file.length() == 0) {
	            	Assert.assertTrue("File is empty", false);
	            }
	            RandomAccessFile raf = new RandomAccessFile(file, "rw");
	            
	            while((str = raf.readLine()) != null){
	            	key = key + str + "\n";
	            }
	        } catch (FileNotFoundException e) {
	            Assert.assertTrue(e.getMessage(), false);
	        } catch (Exception e) {
	            Assert.assertTrue(e.getMessage(), false);
	        }
	        return key;
	    }
	    

}
