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

import org.apache.commons.cli.*;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Arrays;

/**
 * @author Victor Galkin
 */
public class ClientUsage {
    private Commands commands;
    private Options cmdCommonOptions;
    private Options helpOptions;


    public ClientUsage() {
        commands = new Commands();

        cmdCommonOptions = new Options();
        Options listTemplatesOptions = new Options();
        Options listEnvsOptions = new Options();
        Options createEnvOptions = new Options();
        Options describeEnvOptions = new Options();
        Options requestWorkflowOptions = new Options();
        Options cancelWorkflowOptions = new Options();
        Options destroyEnvOptions = new Options();
        helpOptions = new Options();

        commands.put(CommandEnum.LISTTEMPLATES, new Command("listTemplates", listTemplatesOptions, cmdCommonOptions));
        commands.put(CommandEnum.LISTENVS, new Command("listEnvs", listEnvsOptions, cmdCommonOptions));
        commands.put(CommandEnum.CREATEENV, new Command("createEnv", createEnvOptions, cmdCommonOptions));
        commands.put(CommandEnum.DESCRIBEENV, new Command("describeEnv", describeEnvOptions, cmdCommonOptions));
        commands.put(CommandEnum.REQUESTWORKFLOW, new Command("requestWorkflow", requestWorkflowOptions, cmdCommonOptions));
        commands.put(CommandEnum.CANCELWORKFLOW, new Command("cancelWorkflow", cancelWorkflowOptions, cmdCommonOptions));
        commands.put(CommandEnum.DESTROYENV, new Command("destroyEnv", destroyEnvOptions, cmdCommonOptions));

        Option debug = new Option("d", "debug", false, "Debug output");
        cmdCommonOptions.addOption(debug);

        Option server = new Option("s", "server", true, "Server http://host:port");
        server.setArgs(1);
        server.setRequired(true);
        server.setArgName("server");
        server.setType(URL.class);
        cmdCommonOptions.addOption(server);

        Option user = new Option("u", "user", true, "User name");
        user.setArgs(1);
        user.setRequired(false);
        user.setArgName("user");
        cmdCommonOptions.addOption(user);

        Option password = new Option("p", "password", true, "Verbose output");
        password.setArgs(1);
        password.setArgName("password");
        password.setRequired(false);
        cmdCommonOptions.addOption(password);

        Option envName = new Option("e", "environment", true, "Environment name");
        envName.setArgs(1);
        envName.setArgName("environment");
        envName.setRequired(true);
        createEnvOptions.addOption(envName);
        describeEnvOptions.addOption(envName);
        requestWorkflowOptions.addOption(envName);
        cancelWorkflowOptions.addOption(envName);
        destroyEnvOptions.addOption(envName);

        Option creator = new Option("c", "creator", true, "Environment creator");
        creator.setArgs(1);
        creator.setArgName("creator");
        creator.setRequired(true);
        createEnvOptions.addOption(creator);

        Option templateName = new Option("tn", "tn", true, "Template name");
        templateName.setArgs(1);
        templateName.setArgName("name");
        templateName.setRequired(true);
        createEnvOptions.addOption(templateName);

        Option templateVersion = new Option("tv", "tv", true, "Template version");
        templateVersion.setArgs(1);
        templateVersion.setArgName("version");
        templateVersion.setRequired(true);
        createEnvOptions.addOption(templateVersion);

        Option workflowName = new Option("w", "workflow", true, "Workflow name");
        workflowName.setArgs(1);
        workflowName.setArgName("workflow");
        workflowName.setRequired(true);
        requestWorkflowOptions.addOption(workflowName);
        cancelWorkflowOptions.addOption(workflowName);

        Option params = new Option("v", "variables", true, "Variables");
        params.setValueSeparator('=');
        params.setArgs(2);
        params.setRequired(false);
        params.setArgName("variable=value");
        requestWorkflowOptions.addOption(params);
        createEnvOptions.addOption(params);


        Option projectId = new Option("pr", "project", true, "Project Id");
        projectId.setArgs(1);
        projectId.setArgName("project");
        projectId.setRequired(true);
        projectId.setType(Number.class);
        listEnvsOptions.addOption(projectId);
        createEnvOptions.addOption(projectId);

        Option help = new Option("h", "help", true, "Help");
        help.setArgName("command");
        help.setArgs(1);
        help.setRequired(true);
        help.setOptionalArg(true);
        helpOptions.addOption(help);
    }

    public ParseResult parseCommandLine(String[] args) throws ParseException {
        CommandLineParser parser = new GnuParser();

        if (args.length < 1)
            throw new ParseException("No parameters");

        try {
            ParseResult parseResult = new ParseResult(ActionEnum.HELP, parser.parse(helpOptions, args));
            String value = parseResult.commandLine.getOptionValue("help");
            if (value != null) checkCommand(value);
            return parseResult;
        } catch (ParseException ex) {
            if (ex.getMessage().startsWith("Unknown command")) throw ex;
        }

        checkCommand(args[0]);

        ActionEnum action = ActionEnum.valueOf(args[0].toUpperCase());

        if (!commands.containsKey(CommandEnum.valueOf(action.toString())))
            throw new ParseException("Unknown command: " + args[0]);

        String[] croppedArgs;
        if (args.length == 1) croppedArgs = new String[]{};
        else croppedArgs = Arrays.copyOfRange(args, 1, args.length);

        return new ParseResult(action, parser.parse(commands.get(CommandEnum.valueOf(action.toString())).getEffectiveOptions(), croppedArgs));
    }

    private void checkCommand(String value) throws ParseException {
        try {
            CommandEnum.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ParseException("Unknown command: " + value);
        }
    }

    public void printHelp(OutputStream outputStream) {
        printHelp(outputStream, commands, cmdCommonOptions, helpOptions, false);
    }

    public void printCommandHelp(OutputStream outputStream, CommandEnum action) {
        printHelp(outputStream, new Commands(action, commands.get(action)), cmdCommonOptions, helpOptions, true);
    }

    private static void printHelp(OutputStream outputStream, Commands commands, Options cmdCommonOptions, Options helpOptions, boolean verboseOptions) {
        HelpFormatter formatter = new HelpFormatter();
        PrintWriter printWriter = new PrintWriter(outputStream, true);
        formatter.printUsage(printWriter, HelpFormatter.DEFAULT_WIDTH, "client command", cmdCommonOptions);
        formatter.printUsage(printWriter, HelpFormatter.DEFAULT_WIDTH, "client", helpOptions);

        for (CommandEnum command : commands.keySet()) {
            formatter.setSyntaxPrefix("command: ");
            formatter.printUsage(printWriter, HelpFormatter.DEFAULT_WIDTH, commands.get(command).name, commands.get(command).getOptions());
            if (verboseOptions)
                formatter.printOptions(printWriter, HelpFormatter.DEFAULT_WIDTH, commands.get(command).getOptions(), HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD);
        }
    }
}
