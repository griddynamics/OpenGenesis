package com.griddynamics.genesis.tools.environments;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class SshWrapper {

    String hostname;
    String username = "root";
    String password = "";
    String pemPath = "./src/main/resources/rhelimg.pem";

    Connection conn = null;

    public SshWrapper(String hostname) {
        this.hostname = hostname;
    }

    boolean testConnection() throws Exception {
        try {
            /* Create a connection instance */

            conn = new Connection(hostname);

            /* Now connect */

            conn.connect();
            boolean isAuthenticated = conn.authenticateWithPublicKey(username, new File(pemPath), password);

            if (!isAuthenticated) {
                System.out.println("isAuthenticated = false");
                throw new IOException("Exception: authentication failed. Hostname - " + hostname);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return false;
        }

        return true;
    }



    String get_ls() throws IOException {
        if (!conn.isAuthenticationComplete())
            throw new IOException("Authentication failed");

        Session session = conn.openSession();
        session.execCommand("ls -a");
        StreamGobbler stdout = new StreamGobbler(session.getStdout());

        BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
        String out = br.readLine();

        session.close();

        return out;
    }


    String get_command(String command) throws IOException  {
        String response = null;
        if (!conn.isAuthenticationComplete())
            throw new IOException("Authentication failed");

        Session session = conn.openSession();
        session.execCommand(command);
        StreamGobbler stdout = new StreamGobbler(session.getStdout());

        BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

        while (true) {
            String line = br.readLine();
            if (line == null)
                break;
            System.out.println(line);
        }

        session.close();

        return response;
    }

    @Override
    protected void finalize() throws Throwable {

        conn.close();
        super.finalize();

    }
}
