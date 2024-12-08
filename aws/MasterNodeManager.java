package aws;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import java.util.ArrayList;
import java.util.Scanner;
import utils.ConfigLoader;
import java.util.List;
import java.util.logging.Logger;
import utils.SSHExecutor;

public class MasterNodeManager {
    private static final String ROLE_WORKER = "Worker";
    private static final String ROLE_MAIN = "Main";
    private static final String STATE_RUNNING = "running";
    private static final String SELECTION_MANUAL = "manual";
    private static final String SELECTION_AUTO = "auto";

    private static final Logger LOGGER = Logger.getLogger(MasterNodeManager.class.getName());
    private static final Scanner scanner = new Scanner(System.in);

    // Master 노드 승격 프로세스
    public static void startMasterNodePromotion(AmazonEC2 ec2) {

        // 자동 승격 vs 수동 선택 질문
        System.out.println("Do you want to select the new Master node manually or let the system auto-select?");
        System.out.println("Enter 'manual' for manual selection or 'auto' for automatic selection:");
        String userChoice = scanner.nextLine();

        if (SELECTION_MANUAL.equalsIgnoreCase(userChoice)) {
            // 수동 선택 로직
            handleManualMasterSelection(ec2);
        } else if (SELECTION_AUTO.equalsIgnoreCase(userChoice)) {
            // 자동 선택 로직
            String newMasterIp = electNewMaster(ec2);
            if (newMasterIp != null) {
                System.out.println("Automatically selected new Master Node with IP: " + newMasterIp);
                updateNodeConfiguration(ec2, newMasterIp);
            } else {
                System.err.println("No eligible Worker nodes found for promotion to Master.");
            }
        } else {
            System.err.println("Invalid choice. Aborting promotion process.");
        }
    }

    // 수동 선택 로직
    private static void handleManualMasterSelection(AmazonEC2 ec2) {
        List<Instance> workerNodes = getWorkerNodes(ec2);

        // 워커 노드 출력
        displayWorkerNodes(workerNodes);

        // 사용자로부터 선택 요청
        while (true) {
            System.out.println("Enter the Instance ID of the Worker node to promote as the new Master:");
            String selectedInstanceId = scanner.nextLine();

            Instance selectedInstance = workerNodes.stream()
                    .filter(instance -> instance.getInstanceId().equals(selectedInstanceId))
                    .findFirst()
                    .orElse(null);

            if (selectedInstance != null) {
                String newMasterIp = selectedInstance.getPrivateIpAddress();
                System.out.println("Promoting Worker Node (Instance ID: " + selectedInstanceId + ") to Master...");
                updateNodeConfiguration(ec2, newMasterIp);
                break;
            } else {
                System.err.println("Invalid Instance ID. Please try again.");
            }
        }
    }

    // 모든 워커 노드 출력
    private static void displayWorkerNodes(List<Instance> workerNodes) {
        System.out.println("-----------------------------------------------------------------------");
        System.out.printf("%-20s %-15s %-15s %-15s\n", "Instance ID", "Private IP", "Public DNS", "State");
        System.out.println("-----------------------------------------------------------------------");

        for (Instance worker : workerNodes) {
            System.out.printf(
                    "%-20s %-15s %-15s %-15s\n",
                    worker.getInstanceId(),
                    worker.getPrivateIpAddress(),
                    worker.getPublicDnsName() != null ? worker.getPublicDnsName() : "N/A",
                    worker.getState().getName()
            );
        }
        System.out.println("-----------------------------------------------------------------------");
    }

    // 실행 중인 모든 워커 노드 가져오기
    private static List<Instance> getWorkerNodes(AmazonEC2 ec2) {
        return getFilteredInstances(ec2, ROLE_WORKER);
    }

    // 새로운 마스터 노드 선택
    public static String electNewMaster(AmazonEC2 ec2) {
        DescribeInstancesResult response = ec2.describeInstances();

        for (Reservation reservation : response.getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                boolean isWorker = instance.getTags().stream()
                        .anyMatch(tag -> tag.getKey().equalsIgnoreCase("Role") && tag.getValue().equalsIgnoreCase(ROLE_WORKER));
                if (isWorker && STATE_RUNNING.equalsIgnoreCase(instance.getState().getName())) {
                    return instance.getPrivateIpAddress(); // New Master IP 반환
                }
            }
        }
        return null;
    }

    // 설정 파일 업데이트
    public static void updateNodeConfiguration(AmazonEC2 ec2, String newMasterIp) {
        List<Instance> instances = getAllRunningInstances(ec2);

        // Master 노드로 승격될 노드의 Private DNS 이름 가져오기
        String masterPrivateDnsName = instances.stream()
                .filter(instance -> instance.getPrivateIpAddress().equals(newMasterIp))
                .map(Instance::getPrivateDnsName) // Private DNS Name 가져오기
                .findFirst()
                .orElse(null);

        if (masterPrivateDnsName == null) {
            System.err.println("Failed to retrieve Private DNS name for the new Master Node.");
            return;
        }

        // 각 인스턴스의 역할에 따라 설정 파일 업데이트
        for (Instance instance : instances) {
            boolean isMaster = instance.getPrivateIpAddress().equals(newMasterIp);

            // 새 generateConfig 호출
            String newConfig = generateConfig(isMaster, masterPrivateDnsName);
            updateRemoteConfig(instance.getPublicDnsName(), newConfig);
            restartCondorService(instance.getPublicDnsName());

            // 상태 출력
            if (isMaster) {
                System.out.printf("Node %s promoted to Master. Private IP: %s, DNS Name: %s\n",
                        instance.getInstanceId(), newMasterIp, masterPrivateDnsName);
            } else {
                System.out.printf("Node %s updated as Worker. Private IP: %s\n",
                        instance.getInstanceId(), instance.getPrivateIpAddress());
            }
        }

        // 태그 업데이트를 한 번에 처리
        updateInstanceTags(ec2, instances, newMasterIp);
    }

    static List<Instance> getAllRunningInstances(AmazonEC2 ec2) {
        return getFilteredInstances(ec2, null);
    }

    private static String generateConfig(boolean isMaster, String masterPrivateDnsName) {
        StringBuilder config = new StringBuilder();
        config.append("ALLOW_WRITE = *\n");
        config.append("CONDOR_HOST = ").append(masterPrivateDnsName).append("\n");
        if (isMaster) {
            config.append("DAEMON_LIST = MASTER, SCHEDD, STARTD, COLLECTOR, NEGOTIATOR\n");
        } else {
            config.append("DAEMON_LIST = MASTER, STARTD\n");
        }
        return config.toString();
    }

    // 설정 파일 업데이트
    private static void updateRemoteConfig(String publicDns, String configContent) {
        String pemKeyPath = ConfigLoader.getProperty("PEM_KEY_PATH");
        SSHExecutor sshExecutor = new SSHExecutor(pemKeyPath);

        String command = String.format("echo '%s' | sudo tee /etc/condor/config.d/condor_config.local", configContent);
        try {
            sshExecutor.executeCommand(publicDns, command);
            LOGGER.info("Updated config on node: " + publicDns);
        } catch (Exception e) {
            System.err.println("Error updating config on node: " + publicDns + ", Error: " + e.getMessage());
        }
    }

    private static void restartCondorService(String publicDns) {
        String pemKeyPath = ConfigLoader.getProperty("PEM_KEY_PATH");
        SSHExecutor sshExecutor = new SSHExecutor(pemKeyPath);

        String command = "sudo systemctl restart condor";
        try {
            sshExecutor.executeCommand(publicDns, command);
            LOGGER.info("Restarted Condor service on node: " + publicDns);
        } catch (Exception e) {
            System.err.println("Error restarting Condor service on node: " + publicDns + ", Error: " + e.getMessage());
        }
    }

    public static boolean isMasterNode(AmazonEC2 ec2, String instanceId) {
        DescribeInstancesRequest request = new DescribeInstancesRequest()
                .withInstanceIds(instanceId);
        DescribeInstancesResult result = ec2.describeInstances(request);

        Instance instance = result.getReservations().getFirst().getInstances().getFirst();
        return instance.getTags().stream()
                .anyMatch(tag -> tag.getKey().equalsIgnoreCase("Role") && tag.getValue().equalsIgnoreCase(ROLE_MAIN));
    }

    private static List<Instance> getFilteredInstances(AmazonEC2 ec2, String role) {
        List<Instance> filteredInstances = new ArrayList<>();
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        boolean done = false;

        while (!done) {
            DescribeInstancesResult response = ec2.describeInstances(request);
            for (Reservation reservation : response.getReservations()) {
                for (Instance instance : reservation.getInstances()) {
                    boolean matchesRole = role == null || instance.getTags().stream()
                            .anyMatch(tag -> tag.getKey().equalsIgnoreCase("Role") && tag.getValue().equalsIgnoreCase(role));
                    boolean matchesState = instance.getState().getName().equalsIgnoreCase(
                                                MasterNodeManager.STATE_RUNNING);

                    if (matchesRole && matchesState) {
                        filteredInstances.add(instance);
                    }
                }
            }
            request.setNextToken(response.getNextToken());
            done = response.getNextToken() == null;
        }
        return filteredInstances;
    }

    private static void updateInstanceTags(AmazonEC2 ec2, List<Instance> instances, String newMasterIp) {
        for (Instance instance : instances) {
            String role = instance.getPrivateIpAddress().equals(newMasterIp) ? ROLE_MAIN : ROLE_WORKER;

            try {
                // 각 인스턴스에 대해 개별적으로 태그 업데이트
                CreateTagsRequest tagRequest = new CreateTagsRequest()
                        .withResources(instance.getInstanceId())
                        .withTags(new Tag("Role", role));
                ec2.createTags(tagRequest);

                LOGGER.info(String.format("Updated tag [Role=%s] for instance %s", role, instance.getInstanceId()));
            } catch (Exception e) {
                LOGGER.severe(String.format("Failed to update tag [Role=%s] for instance %s: %s",
                        role, instance.getInstanceId(), e.getMessage()));
            }
        }
    }

}