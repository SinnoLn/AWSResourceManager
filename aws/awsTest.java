package aws;

/*
* Cloud Computing
*
* Dynamic Resource Management Tool
* using AWS Java SDK Library
*
*/
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.Tag;
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
import com.amazonaws.services.ec2.model.DryRunSupportedRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.RebootInstancesRequest;
import com.amazonaws.services.ec2.model.RebootInstancesResult;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Filter;
import utils.ConfigLoader;

public class awsTest {

	static AmazonEC2 ec2;
	static String aws_region = ConfigLoader.getProperty("AWS_REGION");

	private static void init() throws Exception {

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
			.withRegion(aws_region)	/* check the region at AWS console */
			.build();
	}

	public static void main(String[] args) throws Exception {

		init();

		Scanner menu = new Scanner(System.in);
		Scanner id_string = new Scanner(System.in);
		int number;

		while(true)
		{
			System.out.println("                                                            ");
			System.out.println("                                                            ");
			System.out.println("------------------------------------------------------------");
			System.out.println("           Amazon AWS Control Panel using SDK               ");
			System.out.println("------------------------------------------------------------");
			System.out.println("  1. list instance                2. available zones        ");
			System.out.println("  3. start instance               4. available regions      ");
			System.out.println("  5. stop instance                6. create instance        ");
			System.out.println("  7. reboot instance              8. list images            ");
			System.out.println("                                 99. quit                   ");
			System.out.println("------------------------------------------------------------");

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
				.comparing(awsTest::getStatePriority) // running > terminated > others
				.thenComparing(awsTest::getRolePriority) // Role=Main > Role=Worker > others
		);

		// 출력 형식 개선
		System.out.println("---------------------------------------------------------------------------------------");
		System.out.printf("%-15s | %-10s | %-15s | %-15s | %-10s\n",
				"Role", "State", "Public IP", "Private IP", "Instance ID");
		System.out.println("---------------------------------------------------------------------------------------");

		for (Instance instance : allInstances) {
			// 태그에서 Role 추출
			String role = instance.getTags().stream()
					.filter(tag -> tag.getKey().equalsIgnoreCase("Role"))
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
				if (tag.getKey().equalsIgnoreCase("Role")) {
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
			// Step 1: Start the instance
			StartInstancesRequest startRequest = new StartInstancesRequest()
					.withInstanceIds(instance_id);
			ec2.startInstances(startRequest);
			System.out.printf("Successfully started instance %s\n", instance_id);

			// Step 2: Wait for the instance to be in "running" state and IPs to be available
			boolean isIpAssigned = false;
			int retryCount = 0;
			String privateIp = null;
			String publicIp = null;

			while (!isIpAssigned && retryCount < 10) { // Retry up to 10 times
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
					Thread.sleep(5000); // Wait 5 seconds before retrying
					retryCount++;
				}
			}

			if (!isIpAssigned) {
				System.err.println("Failed to retrieve IP addresses for the instance within the timeout.");
			} else {
				System.out.printf("Instance Private IP: %s, Public IP: %s\n", privateIp, publicIp);

				// Step 3: Update Condor Pool
				String masterIp = ConfigLoader.getProperty("MASTER_INSTANCE_IP");
				String privateKeyPath = ConfigLoader.getProperty("PRIVATE_KEY_PATH");

				if (masterIp != null && masterIp.equals(privateIp)) {
					System.out.println("This instance is the Master Node. No updates required.");
				} else {
					System.out.println("This instance is a Worker Node. Updating Condor pool...");
					//CondorUpdater.updateCondorPool(masterIp, privateIp, privateKeyPath);
				}
			}

		} catch (AmazonClientException e) {
			System.err.printf("Error starting instance %s: %s\n", instance_id, e.getMessage());
		} catch (InterruptedException e) {
			System.err.printf("Thread interrupted while waiting: %s\n", e.getMessage());
			Thread.currentThread().interrupt(); // Restore the interrupted status
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

	public static void stopInstance(String instance_id) {
		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

		DryRunSupportedRequest<StopInstancesRequest> dry_request =
			() -> {
			StopInstancesRequest request = new StopInstancesRequest()
				.withInstanceIds(instance_id);

			return request.getDryRunRequest();
		};

		try {
			StopInstancesRequest request = new StopInstancesRequest()
				.withInstanceIds(instance_id);

			ec2.stopInstances(request);
			System.out.printf("Successfully stop instance %s\n", instance_id);

			//CondorUpdater.updateCondorPool("masterIp", "workerIp", "privateKeyPath");

		} catch(Exception e)
		{
			System.out.println("Exception: "+e.toString());
		}

	}

	public static void createInstance(String ami_id) {
		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();
		final String keyPairName = ConfigLoader.getProperty("KEY_PAIR_NAME");
		final String securityGroupName = ConfigLoader.getProperty("SECURITY_GROUP_NAME");

		// 새 인스턴스 생성
		RunInstancesRequest run_request = new RunInstancesRequest()
				.withImageId(ami_id)
				.withInstanceType(InstanceType.T2Micro)
				.withMaxCount(1)
				.withMinCount(1)
				.withKeyName(keyPairName)
				.withSecurityGroups(securityGroupName);

		RunInstancesResult run_response = ec2.runInstances(run_request);
		String instanceId = run_response.getReservation().getInstances().get(0).getInstanceId();

		System.out.printf("Successfully started EC2 instance %s based on AMI %s\n", instanceId, ami_id);

		// 새 인스턴스 상태 확인 (대기)
		waitForInstanceRunning(ec2, instanceId);

		// Main 인스턴스 존재 여부 확인
		boolean hasMainInstance = checkMainInstance(ec2);

		// 태그 할당
		String role = hasMainInstance ? "Worker" : "Main";
		assignTagToInstance(ec2, instanceId, role);

		// 태그 반영 상태 확인
		verifyTagAssignment(ec2, instanceId, role);

		System.out.println("Worker instance will connect to Condor master automatically via AMI configuration.");
	}


	public static void rebootInstance(String instance_id) {

		System.out.printf("Rebooting .... %s\n", instance_id);

		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

		try {
			RebootInstancesRequest request = new RebootInstancesRequest()
					.withInstanceIds(instance_id);

				RebootInstancesResult response = ec2.rebootInstances(request);

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
					.anyMatch(tag -> tag.getKey().equalsIgnoreCase("Role")
							&& tag.getValue().equalsIgnoreCase("Main"));
		}
		return false;
	}

	private static void assignTagToInstance(AmazonEC2 ec2, String instanceId, String role) {
		try {
			DescribeInstancesRequest request = new DescribeInstancesRequest().withInstanceIds(instanceId);
			DescribeInstancesResult response = ec2.describeInstances(request);

			Instance instance = response.getReservations().get(0).getInstances().get(0);
			boolean alreadyTagged = instance.getTags().stream()
					.anyMatch(tag -> tag.getKey().equalsIgnoreCase("Role"));

			if (alreadyTagged) {
				System.out.printf("Instance %s already has a Role tag. Skipping tag assignment.\n", instanceId);
				return;
			}

			CreateTagsRequest tagRequest = new CreateTagsRequest()
					.withResources(instanceId)
					.withTags(new Tag("Role", role));
			ec2.createTags(tagRequest);

			System.out.printf("Assigned tag [Role=%s] to instance %s\n", role, instanceId);
		} catch (Exception e) {
			System.err.printf("Failed to assign tag [Role=%s] to instance %s: %s\n", role, instanceId, e.getMessage());
		}
	}

	private static void waitForInstanceRunning(AmazonEC2 ec2, String instanceId) {
		boolean isRunning = false;
		int retries = 0;

		while (!isRunning && retries < 10) { // 최대 10회 시도
			DescribeInstancesRequest request = new DescribeInstancesRequest().withInstanceIds(instanceId);
			DescribeInstancesResult result = ec2.describeInstances(request);

			InstanceStateName state = InstanceStateName.fromValue(
					result.getReservations().get(0).getInstances().get(0).getState().getName());

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

	private static void verifyTagAssignment(AmazonEC2 ec2, String instanceId, String expectedRole) {
		DescribeInstancesRequest request = new DescribeInstancesRequest().withInstanceIds(instanceId);
		DescribeInstancesResult result = ec2.describeInstances(request);

		Instance instance = result.getReservations().get(0).getInstances().get(0);
		boolean tagAssignedCorrectly = instance.getTags().stream()
				.anyMatch(tag -> tag.getKey().equals("Role") && tag.getValue().equals(expectedRole));

		if (tagAssignedCorrectly) {
			System.out.printf("Tag [Role=%s] successfully assigned to instance %s\n", expectedRole, instanceId);
		} else {
			System.err.printf("Failed to assign tag [Role=%s] to instance %s. Current tags: %s\n",
					expectedRole, instanceId, instance.getTags());
		}
	}

	private static String getInstancePrivateIp(AmazonEC2 ec2, String instanceId) {
		DescribeInstancesRequest request = new DescribeInstancesRequest().withInstanceIds(instanceId);
		DescribeInstancesResult result = ec2.describeInstances(request);

		return result.getReservations().getFirst().getInstances().getFirst().getPrivateIpAddress();
	}
}
	