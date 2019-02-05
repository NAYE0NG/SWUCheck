### SWUcheck - 화자인식 전자 출결 앱(학생용)
<hr/>
기존의 전자 출결 앱의 경우, 본인 식별을 학번과 비밀번호로 진행하기 때문에 대리출결이 가능하는 문제점이 존재한다. 이러한 문제점을 보완하고자 목소리로 학생을 구분(화자식별)하여 출석체크를 진행하는 SWUcheck App을 구현하게 되었다.

1. MS AZURE Speaker Recognition Api : 음성식별

   > 1. Verification Profile - Create Profile
   >    - 로그인 시,  Create Profile Api를 사용하여 계정(profile)을 생성한다.
   > 2. Verification Phrase - List All Supported Verification Phrases
   >    - profile 생성 후, 학습 가능한 문구를 가져온다.
   > 3. Verification Profile - Create Enrollment
   >    - 앞서 생성한 사용자의 프로필에 음성을 등록(학습) 한다.
   >    - (학습완료) 이 과정을 3번 반복하여 'Enrolled' 상태값을 얻어낸다. 
   >    - 3번까지 완료되었다면 화자식별이 가능한 프로필이 생성된 것이다.
   > 4. Speaker Recognition - Verification
   >    - 학습한 문구 중 한가지로 사용자의 음성을 식별한다.

2. BLE Advertising : 1:N 통신

   > BLE Advertising기반의 통신으로 SWUcheck(교수용) app과 통신하므로 서버가 필요없다는 장점이 있다.





