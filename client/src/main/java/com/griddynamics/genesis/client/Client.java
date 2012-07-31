/**
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 *   http://www.griddynamics.com
 *
 *   This library is free software; you can redistribute it and/or modify it under the terms of
 *   the GNU Lesser General Public License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or any later version.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *   FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *   DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *   SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *   OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *   Project:     Genesis
 *   Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.client;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class Client {

    static ClientUsage clientUsage = new ClientUsage();
    private final GenesisClient client;
    private final CommandLine cmdLine;

    public static void main(String[] args) throws Exception {

        try {
            ParseResult parseResult = clientUsage.parseCommandLine(args);
            Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            if (parseResult.commandLine.hasOption("debug"))
                root.setLevel(Level.DEBUG);
            else root.setLevel(Level.OFF);
            System.out.println(new Client(parseResult).doAction(parseResult.getAction()));
        } catch (ParseException parseException) {
            clientUsage.printHelp(System.err);
            System.err.println(parseException);
            System.exit(-1);
        }
    }

    private String doAction(ActionEnum action) throws ParseException {
            switch (action) {
                case HELP:
                    return doHelp();
                case LISTTEMPLATES:
                    return doListTemplates();
                case LISTENVS:
                    return doListEnvs();
                case CREATEENV:
                    return doCreateEnv();
                case DESCRIBEENV:
                    return doDescribeEnv();
                case REQUESTWORKFLOW:
                    return doRequestWorkflow();
                case CANCELWORKFLOW:
                    return doCancelWorkflow();
                case DESTROYENV:
                    return doDestroyEnv();
            }
            return "";
    }
    
    private String doHelp() {
        String value = cmdLine.getOptionValue("help");

        if (value == null)
            clientUsage.printHelp(System.out);
        else {
            clientUsage.printCommandHelp(System.out, CommandEnum.valueOf(value.toUpperCase()));
        }
        return "";
    }

    private String getEnv() { return cmdLine.getOptionValue("environment");}
    
    private Number getProject()  throws ParseException {
        return (Number) cmdLine.getParsedOptionValue("project");
    }

    private GenesisClient getClient(CommandLine commandLine) throws ParseException {
        String user = commandLine.getOptionValue("user");
        String password = commandLine.getOptionValue("password");
        URL server = (URL) commandLine.getParsedOptionValue("server");
        return new ClientResorceGenericWrapper(server, user, password);
    }

    public String doListTemplates() {
        return client.listTemplates();
    }

    public String doListEnvs() throws ParseException {
        return client.listEnvs(getProject());
    }

    public String doDescribeEnv() {
        return client.describeEnv(getEnv());
    }

    public String doDestroyEnv() {
        return client.destroyEnv(getEnv());
    }

    private Map<String, String> extractVariables(CommandLine commandLine) {
        Map<String, String> result = new HashMap<String, String>();
        Properties variables = commandLine.getOptionProperties("variables");
        for (Map.Entry<Object, Object> variable : variables.entrySet()) {
            result.put((String) variable.getKey(), (String) variable.getValue());
        }
        return result;
    }

    public String doCreateEnv() throws ParseException {
        String creator = cmdLine.getOptionValue("creator");
        String templateName = cmdLine.getOptionValue("tn");
        String templateVersion = cmdLine.getOptionValue("tv");
        Map<String, String> variables = extractVariables(cmdLine);
        return client.createEnv(getProject(), getEnv(), creator, templateName, templateVersion, variables);
    }

    public String doRequestWorkflow() {
        String workflow = cmdLine.getOptionValue("workflow");
        Map<String, String> variables = extractVariables(cmdLine);
        return client.requestWorkflow(getEnv(), workflow, variables);
    }

    public String doCancelWorkflow() {
        return client.cancelWorkflow(getEnv());
    }

    public Client(ParseResult parseResult) throws ParseException {
        this.cmdLine = parseResult.getCommandLine();
        this.client = getClient(cmdLine);
    }
}
