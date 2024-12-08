package utils;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

public class SSHExecutor {
    private static final Logger LOGGER = Logger.getLogger(SSHExecutor.class.getName());
    private final String pemKeyPath;

    public SSHExecutor(String pemKeyPath) {
        if (pemKeyPath == null || pemKeyPath.isEmpty()) {
            throw new IllegalArgumentException("PEM key path must not be null or empty.");
        }
        this.pemKeyPath = pemKeyPath;
    }

    public String executeCommand(String publicDns, String command) throws Exception {
        Session session = null;
        try {
            session = createSSHSession(publicDns);
            return executeSSHCommand(session, command);
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
                LOGGER.info("Disconnected from node: " + publicDns);
            }
        }
    }

    private Session createSSHSession(String publicDns) throws JSchException {
        JSch jsch = new JSch();
        jsch.addIdentity(pemKeyPath);
        Session session = jsch.getSession("ec2-user", publicDns, 22);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        LOGGER.info("Connected to node: " + publicDns);
        return session;
    }

    private String executeSSHCommand(Session session, String command) throws Exception {
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);
            channel.setErrStream(System.err);

            InputStream input = channel.getInputStream();
            channel.connect();
            LOGGER.info("Executing command: " + command);

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }

            LOGGER.info("Command output: " + output);
            return output.toString();

        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
                LOGGER.info("SSH channel disconnected.");
            }
        }
    }
}
