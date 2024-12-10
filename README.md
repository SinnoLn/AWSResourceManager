# AWSResourceManager

AWSResourceManager는 Amazon Web Services(AWS) SDK를 활용하여 EC2 인스턴스 및 HTCondor 작업 풀을 동적으로 관리하는 도구입니다. 이 프로젝트는 EC2 인스턴스 생성, 시작, 중지, 재부팅, 태그 관리와 HTCondor 작업 풀 업데이트를 포함한 다양한 기능을 제공합니다.

---

## 주요 기능

### 1. EC2 인스턴스 관리
- **인스턴스 목록 보기**  
  모든 EC2 인스턴스의 ID, 상태, 태그(Role), Public/Private IP 주소를 정렬하여 출력합니다.

- **가용 영역 및 리전 조회**  
  사용자가 접근 가능한 AWS 가용 영역(Availability Zones)과 리전을 조회합니다.

- **인스턴스 생성**
  - 새 EC2 인스턴스를 생성합니다.
  - Master 노드가 없으면 새로 생성된 인스턴스에 `Role=Main` 태그를 지정합니다.
  - Master 노드가 이미 존재하면 `Role=Worker` 태그를 지정합니다.

- **인스턴스 시작, 중지 및 재부팅**
  - 인스턴스를 시작, 중지 또는 재부팅합니다.
  - Master 노드를 중지할 경우 자동으로 Worker 노드 승격을 수행합니다.

- **AMI(이미지) 목록 보기**  
  사용 가능한 Amazon Machine Image(AMI)의 ID, 이름 및 소유자 정보를 출력합니다.

---

### 2. HTCondor 작업 풀 업데이트
- **Condor 상태 확인**  
  Master 노드에서 `condor_status` 명령을 실행해 작업 풀 상태를 확인합니다.

- **Master 노드 승격**  
  수동 또는 자동으로 Worker 노드를 Master 노드로 승격합니다.
  - Master 노드 변경 시 Private DNS를 기반으로 Condor 설정 파일을 업데이트합니다.
  - 모든 노드에서 Condor 서비스를 재시작하여 변경 사항을 반영합니다.

---

### 3. EC2 비용 요약
- **비용 조회 기능 추가**  
  AWS Cost Explorer API를 사용하여 EC2 서비스의 월별 비용을 요약합니다.
  - **사용자 입력 기반 날짜 선택:** 사용자가 연도와 월을 입력하여 특정 기간의 비용 데이터를 조회할 수 있습니다.
  - **자동 날짜 범위 설정:** 입력된 달의 1일부터 말일까지의 비용 데이터를 조회합니다.
  - **비용 데이터가 없는 경우 처리:** 비용 데이터가 없을 경우 `$0`로 명시적으로 출력합니다.
  - **서비스별 비용 요약:** 서비스별로 비용 데이터를 그룹화하여 제공합니다.

---

### 4. 인스턴스 성능 모니터링
- **성능 메트릭 조회**  
  AWS CloudWatch API를 사용하여 인스턴스 성능 데이터를 실시간으로 조회합니다.
  - **지원 메트릭:**
    - **CPU 사용률 (%)**: 해당 인스턴스가 소비하는 CPU 자원의 비율을 나타냅니다.
    - **네트워크 수신 (바이트)**: 네트워크로 들어오는 데이터의 양(바이트 기준)을 보여줍니다.
  - **사용자 친화적 출력:** 각 메트릭에 대한 의미와 데이터를 보기 좋게 표시합니다.
  - **종료된 인스턴스 처리:** 실행 중이 아닌 인스턴스를 입력할 경우 명확한 오류 메시지를 표시합니다.

---

### 5. Auto Scaling Group 관리
- **Auto Scaling Group 목록 보기**  
  현재 생성된 Auto Scaling Group의 정보를 조회할 수 있습니다.
  - **출력 정보:**
    - Group 이름
    - 현재 인스턴스 수
    - MinSize, MaxSize
    - DesiredCapacity (원하는 인스턴스 수)
  - 사용자가 Auto Scaling Group 상태를 빠르게 파악할 수 있도록 지원합니다.

- **Auto Scaling Group 구성**  
  사용자가 Auto Scaling Group의 크기를 동적으로 설정할 수 있는 기능을 제공합니다.
  - **사용자 입력:** MinSize, MaxSize, DesiredCapacity 값을 입력받아 설정합니다.
  - **기본값 유지:** 기존 값을 유지하려면 입력 없이 Enter 키를 누릅니다.
  - **새로운 설정:** 설정값 변경 후 Auto Scaling Group이 즉시 업데이트됩니다.

- **인스턴스 스케일링**  
  새로운 DesiredCapacity 값을 기반으로 EC2 인스턴스를 동적으로 생성하거나 종료합니다.
  - **DesiredCapacity > 0:** 지정된 인스턴스 수만큼 생성됩니다.
  - **DesiredCapacity = 0:** 모든 Auto Scaling Group에서 관리되는 인스턴스가 종료됩니다.
  - **스케일링 상태 출력:** 성공 여부와 상태 정보를 명확히 표시합니다.

- **Scaling Policy 적용**  
  동적 크기 조정을 위한 Target Tracking Scaling Policy를 설정할 수 있습니다.
  - **CPU 사용률 기반:** ASGAverageCPUUtilization을 기준으로 Auto Scaling Group의 크기를 조정합니다.
  - **Target 설정:** 기본값으로 CPU 사용률 50%를 목표로 설정합니다.
  - **사용자 선택:** Scaling Policy 활성화 여부를 입력받아 적용합니다.

---

## 설치 및 실행 방법

### 1. 사전 준비
- **AWS 계정 설정**  
  AWS 콘솔에서 액세스 키와 시크릿 키를 생성한 후, `~/.aws/credentials` 파일에 추가합니다.

- **필수 도구 설치**
  - Java 11 이상
  - AWS SDK for Java
  - OpenSSH 클라이언트 (SSH를 통한 EC2 연결에 필요)

- **config.env 파일 생성**  
  프로젝트 루트에 `config.env` 파일을 생성하고 아래 형식에 맞게 작성하세요:
  ```env
  AWS_REGION=ap-northeast-2
  KEY_PAIR_NAME=your-key-pair
  SECURITY_GROUP_NAME=your-security-group
  PEM_KEY_PATH=/path/to/your-key.pem
  ```

---


### **프로젝트에 필요한 AWS 권한**

### IAM 정책 요구 사항
아래의 IAM 정책이 프로젝트에 필요한 권한을 제공합니다. 이 정책들은 프로젝트의 주요 기능 실행을 보장하며, 직접 연결된 AWS 관리형 정책으로 구성되어 있습니다.

| **Policy Name**                           | **Type**       | **Attached via** |
|-------------------------------------------|----------------|-------------------|
| AmazonEC2FullAccess                       | AWS Managed    | Directly          |
| AmazonSSMFullAccess                       | AWS Managed    | Directly          |
| AmazonSSMManagedInstanceCore              | AWS Managed    | Directly          |
| AmazonVPCReadOnlyAccess                   | AWS Managed    | Directly          |
| AutoScalingFullAccess                     | AWS Managed    | Directly          |
| IAMReadOnlyAccess                         | AWS Managed    | Directly          |

### **정책 위치**
`~/.aws/credentials` 파일과 연결된 IAM 역할 또는 사용자 계정에 위의 정책을 할당해야 합니다.

---

### **권한 설명**
- **AmazonEC2FullAccess**: EC2 인스턴스 생성, 중지, 시작, 태그 관리, 성능 모니터링 등.
- **AmazonSSMFullAccess & AmazonSSMManagedInstanceCore**: SSM을 통한 EC2 원격 관리 및 설정.
- **AmazonVPCReadOnlyAccess**: VPC 정보(서브넷, 라우팅 테이블 등) 읽기 전용 액세스.
- **AutoScalingFullAccess**: Auto Scaling Group 생성 및 크기 조정.
- **IAMReadOnlyAccess**: IAM 사용자 및 역할의 읽기 전용 권한.
