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
  MASTER_INSTANCE_IP=
  PEM_KEY_PATH=/path/to/your-key.pem
  ```