### SWUcheck 화자인식 전자 출결 앱(학생용)
<hr/>
> 기존의 전자 출결 앱의 경우, 본인 식별을 학번과 비밀번호로 진행하기 때문에 대리출결이 가능하는 문제점이 존재한다. 이러한 문제점을 보완하고자 화자인식 기능을 갖춘 SWUcheck App을 구현하게 되었다.
> MS AZURE의 Speaker Recognition Api를 사용하여 계정 생성시, 사용자의 음성을 등록(학습)하는 과정을 통해 학생 개개인의 음성 profile을 생성한다.
> BLE Advertising기반의 통신으로 SWUcheck(교수용) app과 통신하므로 서버가 필요없다는 장점이 있다.

- MS AZURE Speaker Recognition Api : 음성식별
- BLE Advertising : 1:N 통신

