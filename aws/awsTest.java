package aws;

/*
* Cloud Computing
*
* Dynamic Resource Management Tool
* using AWS Java SDK Library
*
*/
import static aws.MasterNodeManager.isMasterNode;
import static utils.Constants.*;

import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.AttachInstancesRequest;
import com.amazonaws.services.costexplorer.AWSCostExplorerClient;
import com.amazonaws.services.costexplorer.AWSCostExplorerClientBuilder;
import com.amazonaws.services.costexplorer.model.DateInterval;
import com.amazonaws.services.costexplorer.model.GetCostAndUsageRequest;
import com.amazonaws.services.costexplorer.model.GetCostAndUsageResult;
import com.amazonaws.services.costexplorer.model.Group;
import com.amazonaws.services.costexplorer.model.GroupDefinition;
import com.amazonaws.services.costexplorer.model.ResultByTime;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.RebootInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeRegionsResult;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Filter;
import utils.ConfigLoader;

public class awsTest {

	static AmazonEC2 ec2;

	private static void init() {

		ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();
		try {
			credentialsProvider.getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException(
					"Cannot load the credentials from the credential profiles file. " +
					"Please make sure that your credentials file is at the correct " +
					"location (~/.aws/credentials), and is in valid format.", e);
		}
		ec2 = AmazonEC2ClientBuilder.standard()
			.withCredentials(credentialsProvider)
			.withRegion(AWS_REGION)	/* check the region at AWS console */
			.build();
	}

	public static void main(String[] args) throws Exception {

		init();

		Scanner menu = new Scanner(System.in);
		Scanner id_string = new Scanner(System.in);
		int number;

		while(true)
		{
			System.out.println("                                                                   ");
			System.out.println("                                                                   ");
			System.out.println("-------------------------------------------------------------------");
			System.out.println("                Amazon AWS Control Panel using SDK                 ");
			System.out.println("-------------------------------------------------------------------");
			System.out.println("  1. List Instance                2. Available Zones               ");
			System.out.println("  3. Start Instance               4. Available Regions             ");
			System.out.println("  5. Stop Instance                6. Create Instance               ");
			System.out.println("  7. Reboot Instance              8. List Images                   ");
			System.out.println("  9. Condor Pool Status          10. EC2 Cost Summary              ");
			System.out.println(" 11. List Auto Scaling Groups    12. Monitor Instance Performance  ");
			System.out.println(" 13. Configure Auto Scaling      14. Change Master Node            ");
			System.out.println("                                                                   ");
			System.out.println("                                 99. quit                          ");
			System.out.println("-------------------------------------------------------------------");

			System.out.print("Enter an integer: ");

			if(menu.hasNextInt()){
				number = menu.nextInt();
				}else {
					System.out.println("concentration!");
					break;
				}


			String instance_id = "";

			switch(number) {
			case 1:
				listInstances();
				break;

			case 2:
				availableZones();
				break;

			case 3:
				System.out.print("Enter instance id: ");
				if(id_string.hasNext())
					instance_id = id_string.nextLine();

				if(!instance_id.trim().isEmpty())
					startInstance(instance_id);
				break;

			case 4:
				availableRegions();
				break;

			case 5:
				System.out.print("Enter instance id: ");
				if(id_string.hasNext())
					instance_id = id_string.nextLine();

				if(!instance_id.trim().isEmpty())
					stopInstance(instance_id);
				break;

			case 6:
				System.out.print("Enter ami id: ");
				String ami_id = "";
				if(id_string.hasNext())
					ami_id = id_string.nextLine();

				if(!ami_id.trim().isEmpty())
					createInstance(ami_id);
				break;

			case 7:
				System.out.print("Enter instance id: ");
				if(id_string.hasNext())
					instance_id = id_string.nextLine();

				if(!instance_id.trim().isEmpty())
					rebootInstance(instance_id);
				break;

			case 8:
				listImages();
				break;

			case 9:
				CondorUpdater.listCondorStatus(ec2);
				break;

			case 10:
				getEC2CostSummary();
				break;

			case 11:
				// Auto Scaling Group 목록 출력
				AutoScalingManager.listAutoScalingGroups();
				break;

			case 12:
				System.out.print("Enter instance id to monitor: ");
				if (id_string.hasNext())
					instance_id = id_string.nextLine();

				if (!instance_id.trim().isEmpty())
					PerformanceMonitor.monitorInstancePerformance(instance_id);
				break;

			case 13:
				System.out.print("Enter Auto Scaling Group Name: ");
				String autoScalingGroupName = id_string.nextLine();

				if (!autoScalingGroupName.trim().isEmpty()) {
					AutoScalingManager.configureAutoScaling(autoScalingGroupName);
				}
				break;

			case 14:
				System.out.println("Starting Master Node change process...");
				MasterNodeManager.startMasterNodePromotion(ec2);
				break;

			case 99:
				System.out.println("bye!");
				menu.close();
				id_string.close();
				return;

			default: System.out.println("concentration!");
			}

		}

	}

	public static void listInstances() {
		System.out.println("Listing instances....");
		boolean done = false;

		DescribeInstancesRequest request = new DescribeInstancesRequest();
		List<Instance> allInstances = new ArrayList<>();

		// 모든 인스턴스 수집
		while (!done) {
			DescribeInstancesResult response = ec2.describeInstances(request);

			for (Reservation reservation : response.getReservations()) {
				allInstances.addAll(reservation.getInstances());
			}

			// 다음 페이지의 결과가 있는 경우 설정
			request.setNextToken(response.getNextToken());
			if (response.getNextToken() == null) {
				done = true;
			}
		}

		// 인스턴스 정렬: 상태(running > terminated > others), 태그(Role=Main > Role=Worker > others)
		allInstances.sort(Comparator
				.comparing(awsTest::getStatePriority)
				.thenComparing(awsTest::getRolePriority)
		);

		System.out.println("---------------------------------------------------------------------------------------");
		System.out.printf("%-15s | %-10s | %-15s | %-15s | %-10s\n",
				"Role", "State", "Public IP", "Private IP", "Instance ID");
		System.out.println("---------------------------------------------------------------------------------------");

		for (Instance instance : allInstances) {
			// 태그에서 Role 추출
			String role = instance.getTags().stream()
					.filter(tag -> tag.getKey().equalsIgnoreCase(ROLE_TAG))
					.map(Tag::getValue)
					.findFirst()
					.orElse("Unknown");

			// 주요 데이터 출력
			System.out.printf("%-15s | %-10s | %-15s | %-15s | %-10s\n",
					role,
					instance.getState().getName(),
					instance.getPublicIpAddress() != null ? instance.getPublicIpAddress() : "N/A",
					instance.getPrivateIpAddress() != null ? instance.getPrivateIpAddress() : "N/A",
					instance.getInstanceId());
		}
		System.out.println("---------------------------------------------------------------------------------------");
	}

	// 상태 우선순위를 반환하는 헬퍼 메서드
	private static int getStatePriority(Instance instance) {
		return switch (instance.getState().getName().toLowerCase()) {
			case "running" -> 0; // 가장 높은 우선순위
			case "terminated" -> 1; // 두 번째 우선순위
			default -> 2; // 기타 상태
		};
	}

	// 역할 우선순위를 반환하는 헬퍼 메서드
	private static int getRolePriority(Instance instance) {
		if (instance.getTags() != null) {
			for (Tag tag : instance.getTags()) {
				if (tag.getKey().equalsIgnoreCase(ROLE_TAG)) {
					switch (tag.getValue().toLowerCase()) {
						case "main":
							return 0; // Role=Main이 가장 높은 우선순위
						case "worker":
							return 1; // Role=Worker는 두 번째 우선순위
					}
				}
			}
		}
		return 2; // 태그가 없거나 Role이 지정되지 않은 경우 가장 낮은 우선순위
	}

	public static void availableZones()	{

		System.out.println("Available zones....");
		try {
			DescribeAvailabilityZonesResult availabilityZonesResult = ec2.describeAvailabilityZones();
			Iterator <AvailabilityZone> iterator = availabilityZonesResult.getAvailabilityZones().iterator();

			AvailabilityZone zone;
			while(iterator.hasNext()) {
				zone = iterator.next();
				System.out.printf("[id] %s,  [region] %15s, [zone] %15s\n", zone.getZoneId(), zone.getRegionName(), zone.getZoneName());
			}
			System.out.println("You have access to " + availabilityZonesResult.getAvailabilityZones().size() +
					" Availability Zones.");

		} catch (AmazonServiceException ase) {
				System.out.println("Caught Exception: " + ase.getMessage());
				System.out.println("Reponse Status Code: " + ase.getStatusCode());
				System.out.println("Error Code: " + ase.getErrorCode());
				System.out.println("Request ID: " + ase.getRequestId());
		}

	}

	public static void startInstance(String instance_id) {
		System.out.printf("Starting .... %s\n", instance_id);

		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

		try {
			// Step 1: 인스턴스 시작
			StartInstancesRequest startRequest = new StartInstancesRequest()
					.withInstanceIds(instance_id);
			ec2.startInstances(startRequest);
			System.out.printf("Successfully started instance %s\n", instance_id);

			// Step 2: 인스턴스 상태가 "running"이 되고 IP가 할당될 때까지 대기
			boolean isIpAssigned = false;
			int retryCount = 0;
			String privateIp = null;
			String publicIp = null;

			while (!isIpAssigned && retryCount < 10) { // 최대 10번 재시도
				DescribeInstancesRequest describeRequest = new DescribeInstancesRequest()
						.withInstanceIds(instance_id);
				DescribeInstancesResult describeResult = ec2.describeInstances(describeRequest);

				for (Reservation reservation : describeResult.getReservations()) {
					for (Instance instance : reservation.getInstances()) {
						if (instance.getState().getName().equals(InstanceStateName.Running.toString())) {
							privateIp = instance.getPrivateIpAddress();
							publicIp = instance.getPublicIpAddress();
							System.out.printf("Instance State: %s, Private IP: %s, Public IP: %s\n",
									instance.getState().getName(), privateIp, publicIp);

							if (privateIp != null && publicIp != null) {
								isIpAssigned = true;
								break;
							}
						}
					}
				}

				if (!isIpAssigned) {
					System.out.println("Waiting for instance IPs to be assigned...");
					Thread.sleep(5000); // 5초 대기 후 재시도
					retryCount++;
				}
			}

			if (!isIpAssigned) {
				System.err.println("Failed to retrieve IP addresses for the instance within the timeout.");
			} else {
				System.out.printf("Instance Private IP: %s, Public IP: %s\n", privateIp, publicIp);

				// Step 3: 인스턴스가 Master Node인지 Worker Node인지 확인
				boolean isMaster = isMasterNode(ec2, instance_id);

				if (isMaster) {
					System.out.println("This instance is the Master Node. No updates required.");
				} else {
					System.out.println("This instance is a Worker Node. Updating Condor pool...");
				}
			}

		} catch (AmazonClientException e) {
			System.err.printf("Error starting instance %s: %s\n", instance_id, e.getMessage());
		} catch (InterruptedException e) {
			System.err.printf("Thread interrupted while waiting: %s\n", e.getMessage());
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			System.err.printf("Unexpected error: %s\n", e.getMessage());
		}
	}

	public static void availableRegions() {

		System.out.println("Available regions ....");

		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

		DescribeRegionsResult regions_response = ec2.describeRegions();

		for(Region region : regions_response.getRegions()) {
			System.out.printf(
				"[region] %15s, " +
				"[endpoint] %s\n",
				region.getRegionName(),
				region.getEndpoint());
		}
	}

	public static void stopInstance(String instanceId) {
		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

		// 마스터 노드인지 확인
		boolean isMaster = isMasterNode(ec2, instanceId);
		if (isMaster) {
			// Master 승격 프로세스 시작
			System.out.println("Warning: You are about to stop the Master node.");
			System.out.println("A Worker node must be promoted to Master before stopping the current Master.");

			// Master 승격 처리
			MasterNodeManager.startMasterNodePromotion(ec2);
		}

		// 기존 인스턴스 중지
		try {
			StopInstancesRequest request = new StopInstancesRequest()
					.withInstanceIds(instanceId);
			ec2.stopInstances(request);
			System.out.printf("Successfully stopped instance %s\n", instanceId);
		} catch (Exception e) {
			System.err.printf("Failed to stop instance %s: %s\n", instanceId, e.getMessage());
		}
	}

	public static void createInstance(String amiId) {
		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();
		final String keyPairName = ConfigLoader.getProperty("KEY_PAIR_NAME");
		final String securityGroupName = ConfigLoader.getProperty("SECURITY_GROUP_NAME");

		// 새 인스턴스 생성
		RunInstancesRequest runRequest = new RunInstancesRequest()
				.withImageId(amiId)
				.withInstanceType(InstanceType.T2Micro)
				.withMaxCount(1)
				.withMinCount(1)
				.withKeyName(keyPairName)
				.withSecurityGroups(securityGroupName);

		RunInstancesResult runResponse = ec2.runInstances(runRequest);
		String instanceId = runResponse.getReservation().getInstances().get(0).getInstanceId();

		System.out.printf("Successfully started EC2 instance %s based on AMI %s\n", instanceId, amiId);

		// 새 인스턴스 상태 확인 (대기)
		waitForInstanceRunning(ec2, instanceId);

		// Main 인스턴스 존재 여부 확인
		boolean hasMainInstance = checkMainInstance(ec2);

		// 태그 할당
		String role = hasMainInstance ? ROLE_WORKER : ROLE_MAIN;
		assignTagToInstance(ec2, instanceId, role);

		// 태그 반영 상태 확인
		verifyTagAssignment(ec2, instanceId, role);

		// Auto Scaling 그룹에 추가할지 사용자 입력
		if (hasMainInstance) {
			Scanner scanner = new Scanner(System.in);
			System.out.println("Do you want to add this instance to the Auto Scaling Group? (y/n): ");
			String userInput = scanner.nextLine().trim().toLowerCase();

			if ("y".equals(userInput)) {
				attachInstanceToAutoScalingGroup(instanceId);
			} else {
				System.out.println("Instance will not be added to Auto Scaling Group.");
			}
		} else {
			System.out.println("Main instance created. Not attaching to Auto Scaling Group.");
		}

		System.out.println("Worker instance will connect to Condor master automatically via AMI configuration.");
	}

	/**
	 * Auto Scaling 그룹에 인스턴스 추가
	 */
	private static void attachInstanceToAutoScalingGroup(String instanceId) {
		final AmazonAutoScaling autoScalingClient = AmazonAutoScalingClientBuilder.defaultClient();

		try {
			AttachInstancesRequest attachRequest = new AttachInstancesRequest()
					.withInstanceIds(instanceId)
					.withAutoScalingGroupName("HTCondorWorkerASG");

			autoScalingClient.attachInstances(attachRequest);
			System.out.printf("Instance %s successfully attached to Auto Scaling Group: %s\n", instanceId,
					"HTCondorWorkerASG");
		} catch (Exception e) {
			System.err.printf("Error attaching instance %s to Auto Scaling Group %s: %s\n", instanceId,
					"HTCondorWorkerASG", e.getMessage());
		}
	}


	public static void rebootInstance(String instance_id) {

		System.out.printf("Rebooting .... %s\n", instance_id);

		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

		try {
			RebootInstancesRequest request = new RebootInstancesRequest()
					.withInstanceIds(instance_id);

			ec2.rebootInstances(request);

			System.out.printf(
					"Successfully rebooted instance %s", instance_id);

		} catch(Exception e)
		{
			System.out.println("Exception: "+ e);
		}


	}

	public static void listImages() {
		System.out.println("Listing images....");

		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

		DescribeImagesRequest request = new DescribeImagesRequest();
		ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();

		request.getFilters().add(new Filter("owner-id").withValues("654654419086"));
		request.setRequestCredentialsProvider(credentialsProvider);

		DescribeImagesResult results = ec2.describeImages(request);

		for(Image images :results.getImages()){
			System.out.printf("[ImageID] %s, [Name] %s, [Owner] %s\n",
					images.getImageId(), images.getName(), images.getOwnerId());
		}

	}

	private static boolean checkMainInstance(AmazonEC2 ec2) {
		DescribeInstancesRequest request = new DescribeInstancesRequest();
		boolean hasMain = false;

		while (!hasMain) {
			DescribeInstancesResult response = ec2.describeInstances(request);

			for (Reservation reservation : response.getReservations()) {
				for (Instance instance : reservation.getInstances()) {
					// 상태 로그 출력
					System.out.printf("Checking Instance [id] %s, [state] %s, [tags] %s\n",
							instance.getInstanceId(),
							instance.getState().getName(),
							instance.getTags());

					// running 상태와 Role=Main 태그 확인
					if (isRunningAndMain(instance)) {
						hasMain = true;
						System.out.printf("Main instance found: %s\n", instance.getInstanceId());
						break;
					}
				}

				if (hasMain) break;
			}

			// 다음 페이지 처리
			request.setNextToken(response.getNextToken());
			if (response.getNextToken() == null) {
				break;
			}
		}

		return hasMain;
	}

	// running 상태와 Role=Main 태그 확인을 별도 메서드로 분리
	private static boolean isRunningAndMain(Instance instance) {
		if (instance.getState().getName().equalsIgnoreCase(InstanceStateName.Running.toString())
				&& instance.getTags() != null) {
			return instance.getTags().stream()
					.anyMatch(tag -> tag.getKey().equalsIgnoreCase(ROLE_TAG)
							&& tag.getValue().equalsIgnoreCase(ROLE_MAIN));
		}
		return false;
	}

	private static void assignTagToInstance(
			AmazonEC2 ec2,
			String instanceId,
			String role
	) {
		try {
			DescribeInstancesRequest request = new DescribeInstancesRequest().withInstanceIds(instanceId);
			DescribeInstancesResult response = ec2.describeInstances(request);

			Instance instance = response.getReservations().getFirst().getInstances().getFirst();
			boolean alreadyTagged = instance.getTags().stream()
					.anyMatch(tag -> tag.getKey().equalsIgnoreCase(ROLE_TAG));

			if (alreadyTagged) {
				System.out.printf("Instance %s already has a Role tag. Skipping tag assignment.\n", instanceId);
				return;
			}

			CreateTagsRequest tagRequest = new CreateTagsRequest()
					.withResources(instanceId)
					.withTags(new Tag(ROLE_TAG, role));
			ec2.createTags(tagRequest);

			System.out.printf("Assigned tag [Role=%s] to instance %s\n", role, instanceId);
		} catch (Exception e) {
			System.err.printf("Failed to assign tag [Role=%s] to instance %s: %s\n", role, instanceId, e.getMessage());
		}
	}

	private static void waitForInstanceRunning(
			AmazonEC2 ec2,
			String instanceId
	) {
		boolean isRunning = false;
		int retries = 0;

		while (!isRunning && retries < 10) { // 최대 10회 시도
			DescribeInstancesRequest request = new DescribeInstancesRequest().withInstanceIds(instanceId);
			DescribeInstancesResult result = ec2.describeInstances(request);

			InstanceStateName state = InstanceStateName.fromValue(
					result.getReservations().getFirst().getInstances().getFirst().getState().getName());

			System.out.printf("Waiting for instance %s to be running. Current state: %s\n", instanceId, state);

			if (state.equals(InstanceStateName.Running)) {
				isRunning = true;
			} else {
				try {
					Thread.sleep(5000); // 5초 대기
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					System.err.println("Interrupted while waiting for instance to run.");
				}
			}

			retries++;
		}

		if (!isRunning) {
			System.err.printf("Instance %s did not enter running state within the timeout period.\n", instanceId);
		}
	}

	private static void verifyTagAssignment(
			AmazonEC2 ec2,
			String instanceId,
			String expectedRole
	) {
		DescribeInstancesRequest request = new DescribeInstancesRequest().withInstanceIds(instanceId);
		DescribeInstancesResult result = ec2.describeInstances(request);

		Instance instance = result.getReservations().getFirst().getInstances().getFirst();
		boolean tagAssignedCorrectly = instance.getTags().stream()
				.anyMatch(tag -> tag.getKey().equals(ROLE_TAG) && tag.getValue().equals(expectedRole));

		if (tagAssignedCorrectly) {
			System.out.printf("Tag [Role=%s] successfully assigned to instance %s\n", expectedRole, instanceId);
		} else {
			System.err.printf("Failed to assign tag [Role=%s] to instance %s. Current tags: %s\n",
					expectedRole, instanceId, instance.getTags());
		}
	}

	public static void getEC2CostSummary() {
		try {
			// Cost Explorer 클라이언트 초기화
			AWSCostExplorerClient costExplorerClient = (AWSCostExplorerClient) AWSCostExplorerClientBuilder
					.standard()
					.withRegion(AWS_REGION) // AWS 리전 설정
					.build();

			// 사용자 입력을 통해 조회할 연도와 월을 선택
			Scanner scanner = new Scanner(System.in);
			System.out.print("Enter year (e.g., 2024): ");
			int year = scanner.nextInt();
			System.out.print("Enter month (1-12): ");
			int month = scanner.nextInt();

			// 현재 날짜 확인
			LocalDate today = LocalDate.now();

			// 조회 기간 설정
			LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
			LocalDate lastDayOfMonth;

			if (year == today.getYear() && month == today.getMonthValue()) {
				// 현재 달이면 오늘 날짜를 마지막 날로 설정
				lastDayOfMonth = today;
			} else {
				// 선택한 달의 마지막 날짜 계산
				lastDayOfMonth = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth());
			}

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			String startDate = firstDayOfMonth.format(formatter);
			String endDate = lastDayOfMonth.format(formatter);

			// 요청 생성
			GetCostAndUsageRequest request = new GetCostAndUsageRequest()
					.withTimePeriod(new DateInterval()
							.withStart(startDate)
							.withEnd(endDate))
					.withGranularity("MONTHLY")
					.withMetrics("BlendedCost")
					.withGroupBy(new GroupDefinition()
							.withType("DIMENSION")
							.withKey("SERVICE"));

			// API 호출
			GetCostAndUsageResult result = costExplorerClient.getCostAndUsage(request);

			// 결과 출력
			System.out.println("                     ");
			System.out.println("AWS EC2 Cost Summary");
			System.out.println("--------------------------------------------------");
			System.out.println("Start Date: " + startDate);
			System.out.println("End Date: " + endDate);

			if (result.getResultsByTime().isEmpty()) {
				System.out.println("No cost data available for the specified time period.");
				return;
			}

			for (ResultByTime resultByTime : result.getResultsByTime()) {
				for (Group group : resultByTime.getGroups()) {
					String serviceName = group.getKeys().getFirst(); // 서비스 이름
					String cost = group.getMetrics().get("BlendedCost").getAmount(); // 비용

					// 비용이 없으면 0원 출력
					if (cost == null || cost.isEmpty() || Double.parseDouble(cost) == 0.0) {
						System.out.printf("Service: %s, Cost: $0\n", serviceName);
					} else {
						System.out.printf("Service: %s, Cost: $%s\n", serviceName, cost);
					}
				}
			}
		} catch (AmazonServiceException e) {
			System.err.println("AWS service error: " + e.getMessage());
		} catch (AmazonClientException e) {
			System.err.println("AWS client error: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Failed to retrieve cost data: " + e.getMessage());
		}
	}
}
	