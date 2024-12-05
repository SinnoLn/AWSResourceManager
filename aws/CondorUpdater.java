package aws;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;
import utils.ConfigLoader;

public class CondorUpdater {
    private static final Logger LOGGER = Logger.getLogger(CondorUpdater.class.getName());

    // Condor Pool 상태 조회
    public static void listCondorStatus() {
        String masterIp = ConfigLoader.getProperty("MASTER_INSTANCE_IP");
        String privateKeyPath = ConfigLoader.getProperty("PRIVATE_KEY_PATH");

        // Validate configuration
        if (masterIp == null || privateKeyPath == null) {
            System.err.println("Master IP or private key path is not set in the configuration.");
            return;
        }

        Session session = null;
        try {
            LOGGER.info("Connecting to master node...");

            // Initialize JSch session
            JSch jsch = new JSch();
            jsch.addIdentity(privateKeyPath);
            session = jsch.getSession("ec2-user", masterIp, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            LOGGER.info("Connected to master node.");

            // condor_status 명령 실행
            String command = "condor_status";
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);
            channel.setErrStream(System.err);

            InputStream input = channel.getInputStream();
            channel.connect();

            LOGGER.info("Executing condor_status command...");

            // 명령 결과 출력
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                String line;
                System.out.println("Condor Pool Status:");
                System.out.println("-------------------");
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            channel.disconnect();
            LOGGER.info("Command execution completed and channel disconnected.");

        } catch (JSchException e) {
            System.err.println("SSH connection error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error executing condor_status: " + e.getMessage());
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
                LOGGER.info("Disconnected from master node.");
            }
        }
    }
}