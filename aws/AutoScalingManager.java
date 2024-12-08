package aws;

import static utils.Constants.*;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.*;
import java.util.Scanner;

public class AutoScalingManager {

    public static void listAutoScalingGroups() {
        AmazonAutoScaling autoScalingClient = AmazonAutoScalingClientBuilder.standard()
                .withRegion(AWS_REGION)
                .build();

        try {
            System.out.println("Retrieving Auto Scaling Groups...");
            DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest();
            DescribeAutoScalingGroupsResult result = autoScalingClient.describeAutoScalingGroups(request);

            // Auto Scaling 그룹이 없을 경우 처리
            if (result.getAutoScalingGroups().isEmpty()) {
                System.out.println("No Auto Scaling Groups found.");
                return;
            }

            // Auto Scaling 그룹 출력
            System.out.println("Available Auto Scaling Groups:");
            for (AutoScalingGroup group : result.getAutoScalingGroups()) {
                System.out.printf("- Name: %s, Instances: %d, MinSize: %d, MaxSize: %d, DesiredCapacity: %d\n",
                        group.getAutoScalingGroupName(),
                        group.getInstances().size(),
                        group.getMinSize(),
                        group.getMaxSize(),
                        group.getDesiredCapacity());
            }
        } catch (Exception e) {
            System.err.println("Error retrieving Auto Scaling Groups: " + e.getMessage());
        }
    }

    public static void configureAutoScaling(String autoScalingGroupName) {
        AmazonAutoScaling autoScalingClient = AmazonAutoScalingClientBuilder.standard()
                .withRegion(AWS_REGION)
                .build();

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.printf("Starting configuration for Auto Scaling Group: %s\n", autoScalingGroupName);

            // Auto Scaling 그룹의 현재 설정 확인
            DescribeAutoScalingGroupsRequest describeRequest = new DescribeAutoScalingGroupsRequest()
                    .withAutoScalingGroupNames(autoScalingGroupName);
            DescribeAutoScalingGroupsResult describeResult = autoScalingClient.describeAutoScalingGroups(describeRequest);

            if (describeResult.getAutoScalingGroups().isEmpty()) {
                System.err.println("Error: Auto Scaling Group not found.");
                return;
            }

            AutoScalingGroup group = describeResult.getAutoScalingGroups().get(0);
            int currentDesiredCapacity = group.getDesiredCapacity();
            int currentMinSize = group.getMinSize();
            int currentMaxSize = group.getMaxSize();

            // 현재 설정 출력
            System.out.println("Current Configuration:");
            System.out.printf(" - MinSize: %d\n", currentMinSize);
            System.out.printf(" - MaxSize: %d\n", currentMaxSize);
            System.out.printf(" - DesiredCapacity: %d\n", currentDesiredCapacity);

            // 사용자 입력
            int newDesiredCapacity = getUserInput(scanner, "Enter new Desired Capacity (or press Enter to keep current): ", currentDesiredCapacity);
            int newMinSize = getUserInput(scanner, "Enter new MinSize (or press Enter to keep current): ", currentMinSize);
            int newMaxSize = getUserInput(scanner, "Enter new MaxSize (or press Enter to keep current): ", currentMaxSize);

            // Auto Scaling 그룹 업데이트
            UpdateAutoScalingGroupRequest updateRequest = new UpdateAutoScalingGroupRequest()
                    .withAutoScalingGroupName(autoScalingGroupName)
                    .withDesiredCapacity(newDesiredCapacity)
                    .withMinSize(newMinSize)
                    .withMaxSize(newMaxSize);

            autoScalingClient.updateAutoScalingGroup(updateRequest);

            // 새로운 설정 적용 결과 출력
            System.out.println("Updated Auto Scaling Group:");
            System.out.printf(" - MinSize: %d\n", newMinSize);
            System.out.printf(" - MaxSize: %d\n", newMaxSize);
            System.out.printf(" - DesiredCapacity: %d\n", newDesiredCapacity);

            // 새로운 Desired Capacity에 따라 인스턴스 시작
            if (newDesiredCapacity > 0) {
                System.out.println("Ensuring instances are created based on the new Desired Capacity...");
                scaleInstances(autoScalingClient, autoScalingGroupName, newDesiredCapacity);
            } else {
                System.out.println("No instances will be created since Desired Capacity is set to 0.");
            }
        } catch (Exception e) {
            System.err.println("Error configuring auto-scaling: " + e.getMessage());
        }
    }

    private static void scaleInstances(AmazonAutoScaling autoScalingClient, String autoScalingGroupName, int desiredCapacity) {
        try {
            // Auto Scaling 그룹의 DesiredCapacity를 기반으로 인스턴스를 시작
            SetDesiredCapacityRequest capacityRequest = new SetDesiredCapacityRequest()
                    .withAutoScalingGroupName(autoScalingGroupName)
                    .withDesiredCapacity(desiredCapacity)
                    .withHonorCooldown(false);

            autoScalingClient.setDesiredCapacity(capacityRequest);
            System.out.printf("Successfully set Desired Capacity to %d. Instances are being created.\n", desiredCapacity);
        } catch (Exception e) {
            System.err.println("Error scaling instances: " + e.getMessage());
        }
    }

    private static int getUserInput(Scanner scanner, String message, int defaultValue) {
        System.out.print(message);
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return defaultValue; // 사용자가 아무 값도 입력하지 않으면 기본값 유지
        }
        try {
            return Integer.parseInt(input); // 숫자 값으로 변환
        } catch (NumberFormatException e) {
            System.err.println("Invalid input. Using default value.");
            return defaultValue; // 입력이 잘못된 경우 기본값 유지
        }
    }
}