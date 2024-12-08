package aws;

import static utils.Constants.*;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import utils.ConfigLoader;
import utils.SSHExecutor;

public class CondorUpdater {

    // Main 태그 인스턴스의 Public DNS 가져오기
    private static String getMainInstancePublicDns(AmazonEC2 ec2) {
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        DescribeInstancesResult response = ec2.describeInstances(request);

        for (Reservation reservation : response.getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                // 태그와 상태 조건 확인
                boolean isMainTag = instance.getTags().stream()
                        .anyMatch(tag -> tag.getKey().equalsIgnoreCase("Role") && tag.getValue().equalsIgnoreCase("Main"));
                if (isMainTag && STATE_RUNNING.equalsIgnoreCase(instance.getState().getName())) {
                    return instance.getPublicDnsName(); // Public DNS 반환
                }
            }
        }
        return null; // 없을 경우 null 반환
    }

    // Condor 상태 확인
    public static void listCondorStatus(AmazonEC2 ec2) {
        String pemKeyPath = ConfigLoader.getProperty(PEM_KEY_PATH);
        String publicDns = getMainInstancePublicDns(ec2);

        if (publicDns == null) {
            System.err.println("No running instance with Role=Main found.");
            return;
        }

        SSHExecutor sshExecutor = new SSHExecutor(pemKeyPath);

        try {
            String output = sshExecutor.executeCommand(publicDns, CONDOR_STATUS_COMMAND);
            System.out.println("                                                                                                              ");
            System.out.println("Condor Pool Status");
            System.out.println("--------------------------------------------------------------------------------------------------------------");
            System.out.println(output);
        } catch (Exception e) {
            System.err.println("Error retrieving Condor status: " + e.getMessage());
        }
    }

}