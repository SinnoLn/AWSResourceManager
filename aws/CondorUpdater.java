package aws;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import utils.ConfigLoader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

public class CondorUpdater {
    private static final Logger LOGGER = Logger.getLogger(CondorUpdater.class.getName());

    // Main 태그 인스턴스의 Public DNS 가져오기
    private static String getMainInstancePublicDns(AmazonEC2 ec2) {
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        DescribeInstancesResult response = ec2.describeInstances(request);

        for (Reservation reservation : response.getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                // 태그와 상태 조건 확인
                boolean isMainTag = instance.getTags().stream()
                        .anyMatch(tag -> tag.getKey().equalsIgnoreCase("Role") && tag.getValue().equalsIgnoreCase("Main"));
                if (isMainTag && "running".equalsIgnoreCase(instance.getState().getName())) {
                    return instance.getPublicDnsName(); // Public DNS 반환
                }
            }
        }
        return null; // 없을 경우 null 반환
    }

    // Condor 상태 확인
    public static void listCondorStatus(AmazonEC2 ec2) {
        String pemKeyPath = ConfigLoader.getProperty("PEM_KEY_PATH");
        String publicDns = getMainInstancePublicDns(ec2);

        if (publicDns == null) {
            System.err.println("No running instance with Role=Main found.");
            return;
        }

        if (pemKeyPath == null || pemKeyPath.isEmpty()) {
            System.err.println("PEM key path is not set in the configuration.");
            return;
        }

        Session session = null;
        try {
            LOGGER.info("Connecting to main node: " + publicDns);

            // Initialize SSH session
            JSch jsch = new JSch();
            jsch.addIdentity(pemKeyPath);
            session = jsch.getSession("ec2-user", publicDns, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            LOGGER.info("Connected to main node.");

            // Execute condor_status
            String command = "condor_status";
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);
            channel.setErrStream(System.err);

            InputStream input = channel.getInputStream();
            channel.connect();

            LOGGER.info("Executing condor_status command...");

            // Read command output
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
                LOGGER.info("Disconnected from main node.");
            }
        }
    }
}