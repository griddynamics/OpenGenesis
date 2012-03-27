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
 *   @Project:     Genesis
 *   @Description: Execution Workflow Engine
 */
package com.griddynamics.genesis.client;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class Client {

    static ClientUsage clientUsage = new ClientUsage();

    public static void main(String[] args) throws Exception {

        try {
            ParseResult parseResult = clientUsage.parseCommandLine(args);
            Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            if (parseResult.commandLine.hasOption("debug"))
                root.setLevel(Level.DEBUG);
            else root.setLevel(Level.OFF);
            switch (parseResult.getAction()) {
                case HELP:
                    doHelp(parseResult.getCommandLine());
                    break;
                case LISTTEMPLATES:
                    doListTemplates(parseResult.getCommandLine());
                    break;
                case LISTENVS:
                    doListEnvs(parseResult.getCommandLine());
                    break;
                case CREATEENV:
                    doCreateEnv(parseResult.getCommandLine());
                    break;
                case DESCRIBEENV:
                    doDescribeEnv(parseResult.getCommandLine());
                    break;
                case REQUESTWORKFLOW:
                    doRequestWorkflow(parseResult.getCommandLine());
                    break;
                case CANCELWORKFLOW:
                    doCancelWorkflow(parseResult.getCommandLine());
                    break;
                case DESTROYENV:
                    doDestroyEnv(parseResult.getCommandLine());
                    break;
            }
        } catch (ParseException parseException) {
            clientUsage.printHelp(System.err);
            System.err.println(parseException);
            System.exit(-1);
        }
    }

    public static void doHelp(CommandLine commandLine) {
        String value = commandLine.getOptionValue("help");
        if (value == null)
            clientUsage.printHelp(System.out);
        else {
            clientUsage.printCommandHelp(System.out, CommandEnum.valueOf(value.toUpperCase()));
        }
    }

    private static GenesisClient getClient(CommandLine commandLine) {
        String user = commandLine.getOptionValue("user");
        String password = commandLine.getOptionValue("password");
        String server = commandLine.getOptionValue("server");
        return new ClientResorceGenericWrapper(server, user, password);
    }

    public static void doListTemplates(CommandLine commandLine) {
        String result = getClient(commandLine).listTemplates();
        System.out.println(result);
    }

    public static void doListEnvs(CommandLine commandLine) {
        String result = getClient(commandLine).listEnvs();
        System.out.println(result);
    }

    public static void doDescribeEnv(CommandLine commandLine) {
        String environment = commandLine.getOptionValue("environment");
        String result = getClient(commandLine).describeEnv(environment);
        System.out.println(result);
    }

    public static void doDestroyEnv(CommandLine commandLine) {
        String environment = commandLine.getOptionValue("environment");
        String result = getClient(commandLine).destroyEnv(environment);
        System.out.println(result);
    }

    private static Map<String, String> extractVariables(CommandLine commandLine) {
        Map<String, String> result = new HashMap<String, String>();
        Properties variables = commandLine.getOptionProperties("variables");
        for (Map.Entry<Object, Object> variable : variables.entrySet()) {
            result.put((String) variable.getKey(), (String) variable.getValue());
        }
        return result;
    }

    public static void doCreateEnv(CommandLine commandLine) {
        String environment = commandLine.getOptionValue("environment");
        String creator = commandLine.getOptionValue("creator");
        String templateName = commandLine.getOptionValue("tn");
        String templateVersion = commandLine.getOptionValue("tv");
        Map<String, String> variables = extractVariables(commandLine);
        String result = getClient(commandLine).createEnv(environment, creator, templateName, templateVersion, variables);
        System.out.println(result);
    }

    public static void doRequestWorkflow(CommandLine commandLine) {
        String environment = commandLine.getOptionValue("environment");
        String workflow = commandLine.getOptionValue("workflow");
        Map<String, String> variables = extractVariables(commandLine);
        String result = getClient(commandLine).requestWorkflow(environment, workflow, variables);
        System.out.println(result);
    }

    public static void doCancelWorkflow(CommandLine commandLine) {
        String environment = commandLine.getOptionValue("environment");
        String result = getClient(commandLine).cancelWorkflow(environment);
        System.out.println(result);
    }

    public Client() {
    }
}
