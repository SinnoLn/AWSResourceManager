# AWSResourceManager

AWSResourceManager는 Amazon Web Services(AWS) SDK를 활용하여 동적으로 EC2 인스턴스를 관리하고, HTCondor 작업 풀을 업데이트하는 도구입니다. 이 프로젝트는 EC2 인스턴스 생성, 시작, 중지, 재부팅 및 태그 할당을 포함한 다양한 기능을 제공합니다. HTCondor 작업 풀에 새로운 Worker 노드를 추가하고 관리하는 기능도 포함되어 있습니다.

---

## 주요 기능

### 1. EC2 인스턴스 관리
- **인스턴스 목록 보기**  
  AWS에 존재하는 모든 EC2 인스턴스의 ID, 상태, 태그, IP 주소 등을 확인합니다.

- **가용 영역 및 리전 보기**  
  AWS 계정에서 접근 가능한 가용 영역(Availability Zones)과 리전을 조회합니다.

- **인스턴스 생성**
    - 새로운 EC2 인스턴스를 생성합니다.
    - 기존 인스턴스 중 `Role=Main` 태그가 없으면 생성된 인스턴스를 `Main`으로 태그 지정합니다.
    - `Role=Main`이 이미 존재하면 새로운 인스턴스는 `Worker`로 태그 지정됩니다.

- **인스턴스 시작/중지/재부팅**
    - 특정 인스턴스를 시작, 중지 또는 재부팅합니다.

- **이미지 목록 보기**  
  소유한 AMI(이미지)의 목록을 출력합니다.

---

## HTCondor 작업 풀 업데이트
- EC2 인스턴스 생성 시, 생성된 인스턴스가 Worker 노드로 설정되어 HTCondor 작업 풀에 추가됩니다.
- Master 노드와 Worker 노드 간 통신을 설정하고 Condor 서비스를 재시작하여 변경 사항을 반영합니다.

---

## 설치 및 실행 방법

### 1. 사전 준비
- **AWS 계정**  
  AWS 콘솔에서 액세스 키 및 시크릿 키를 생성하고 `~/.aws/credentials`에 설정합니다.

- **AWS SDK 및 Java 설치**
    - Java 11 이상 설치
    - AWS SDK for Java 라이브러리

- **config.env 파일 준비**  
  `config.env` 파일을 프로젝트 루트에 생성하고 아래와 같이 작성하세요:
  ```env
  AWS_REGION=
  KEY_PAIR_NAME=
  SECURITY_GROUP_NAME=default
  MASTER_INSTANCE_IP=
  PRIVATE_KEY_PATH=/path/to/yourkey.pem
  ```

# 프로젝트 빌드

    javac -cp "path/to/aws-sdk.jar" -d out src/aws/*.java src/utils/*.java

# 실행
    java -cp "out:path/to/aws-sdk.jar" aws.awsTest
