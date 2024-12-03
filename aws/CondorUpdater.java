package aws;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CondorUpdater {

    private static final Logger LOGGER = Logger.getLogger(CondorUpdater.class.getName());

    public static void updateCondorPool(
            String masterIp,
            String workerIp,
            String privateKeyPath
    ) {
        if (masterIp == null || masterIp.isEmpty() || workerIp == null || workerIp.isEmpty() || privateKeyPath == null || privateKeyPath.isEmpty()) {
            LOGGER.log(Level.SEVERE, "Invalid arguments: masterIp, workerIp, and privateKeyPath must not be null or empty.");
            return;
        }

        Session session = null;
        try {
            LOGGER.log(Level.INFO, "Connecting to master node at IP: {0}", masterIp);

            JSch jsch = new JSch();
            jsch.addIdentity(privateKeyPath);
            session = jsch.getSession("ec2-user", masterIp, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            // Update the condor pool
            executeCommand(session, String.format("echo 'Worker: %s' >> /etc/condor/condor_config.local", workerIp));

            // Restart the condor service
            executeCommand(session, "sudo condor_restart");

            session.disconnect();
            System.out.println("Condor pool updated successfully");
        } catch (JSchException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error updating Condor pool: {0}", e.getMessage());
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
                LOGGER.log(Level.INFO, "Disconnected from master node.");
            }
        }
    }

    private static void executeCommand(
            Session session,
            String command
    ) throws JSchException, IOException {
        LOGGER.log(Level.INFO, "Executing command: {0}", command);

        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);
            channel.setErrStream(System.err);

            InputStream input = channel.getInputStream();
            channel.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }

            LOGGER.log(Level.INFO, "Command output: {0}", output);

        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
                LOGGER.log(Level.INFO, "Command execution completed and channel disconnected.");
            }
        }
    }
}
