package aws;

import static aws.awsTest.ec2;
import static utils.Constants.AWS_REGION;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricDataResult;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.amazonaws.services.cloudwatch.model.MetricDataQuery;
import com.amazonaws.services.cloudwatch.model.MetricStat;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

public class PerformanceMonitor {
    public static void monitorInstancePerformance(String instanceId) {
        try {
            // 인스턴스 상태 확인
            DescribeInstancesRequest request = new DescribeInstancesRequest().withInstanceIds(instanceId);
            DescribeInstancesResult response = ec2.describeInstances(request);
            Instance instance = response.getReservations().getFirst().getInstances().getFirst();

            if (!"running".equalsIgnoreCase(instance.getState().getName())) {
                System.err.printf("Instance %s is not in running state (current state: %s).\n",
                        instanceId, instance.getState().getName());
                return;
            }

            // CloudWatch 클라이언트를 초기화
            AmazonCloudWatch cloudWatch = AmazonCloudWatchClientBuilder.standard()
                    .withRegion(AWS_REGION)
                    .build();

            // CPU 사용률 및 네트워크 입력 메트릭 수집 및 출력
            System.out.printf("Performance Metrics for Instance: %s\n", instanceId);
            System.out.println("==================================================");

            // CPU 사용률 메트릭
            System.out.println("[CPU Usage (%)]");
            fetchMetricData(cloudWatch, instanceId, "CPUUtilization", "CPU Usage (%)",
                    "Value represents the percentage of allocated CPU that is currently in use.");

            // 네트워크 입력 메트릭
            System.out.println("[Network In (Bytes)]");
            fetchMetricData(cloudWatch, instanceId, "NetworkIn", "Network In (Bytes)",
                    "Value represents the amount of data received by the instance from the network, in bytes.");

        } catch (AmazonServiceException e) {
            System.err.println("AWS service error: " + e.getMessage());
        } catch (AmazonClientException e) {
            System.err.println("AWS client error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Failed to retrieve performance metrics: " + e.getMessage());
        }
    }

    private static void fetchMetricData(
            AmazonCloudWatch cloudWatch,
            String instanceId,
            String metricName,
            String displayName,
            String note
    ) {
        try {
            GetMetricDataRequest request = new GetMetricDataRequest()
                    .withMetricDataQueries(
                            new MetricDataQuery()
                                    .withId("m1")
                                    .withMetricStat(new MetricStat()
                                            .withMetric(new Metric()
                                                    .withNamespace("AWS/EC2")
                                                    .withMetricName(metricName)
                                                    .withDimensions(new Dimension()
                                                            .withName("InstanceId")
                                                            .withValue(instanceId)))
                                            .withStat("Average")
                                            .withPeriod(60)))
                    .withStartTime(Date.from(Instant.now().minus(10, ChronoUnit.MINUTES)))
                    .withEndTime(Date.from(Instant.now()));

            GetMetricDataResult result = cloudWatch.getMetricData(request);

            if (result.getMetricDataResults().isEmpty() ||
                    result.getMetricDataResults().getFirst().getValues().isEmpty()) {
                System.out.println("No data available for " + displayName);
            } else {
                List<Double> values = result.getMetricDataResults().getFirst().getValues();
                List<Date> timestamps = result.getMetricDataResults().getFirst().getTimestamps();

                for (int i = 0; i < values.size(); i++) {
                    System.out.printf("Timestamp: %s - Value: %.2f\n",
                            timestamps.get(i), values.get(i));
                }
            }

            // Note about the metric
            System.out.printf("Note: %s\n", note);
            System.out.println("--------------------------------------------------");

        } catch (Exception e) {
            System.err.printf("Error fetching %s data: %s\n", displayName, e.getMessage());
        }
    }

}
