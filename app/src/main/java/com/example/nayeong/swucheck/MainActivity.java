package com.example.nayeong.swucheck;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.IntentCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    static final String HOST = "westus.api.cognitive.microsoft.com";
    static final String CON_TYPE = "application/json";
    static final String API_KEY ="236f82fc1810454c8c95e75a5c33d641";

    //azure private user profile
    String userProfile = null;

    ProgressDialog progress;

    EditText swuId, swuPwd;
    Button loginBtn;
    String loginId, loginPwd;

    SharedPreferences setting;
    SharedPreferences.Editor editor;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swuId = (EditText) findViewById(R.id.swuId);
        swuPwd = (EditText) findViewById(R.id.swuPwd);
        loginBtn = (Button) findViewById(R.id.loginBtn);

        //auto login
        setting = getSharedPreferences("setting", 0);

        loginId = setting.getString("swuId",null);
        loginPwd = setting.getString("swuPwd",null);
        userProfile= setting.getString("userProfile",null);


        //자동로그인 전부터 외부저장소 권한 동적 확인
        checkPermission();



        //자동로그인이면 페이지 이동
        if(loginId !=null && loginPwd != null && userProfile !=null) {
            Toast.makeText(MainActivity.this, loginId +"님 자동로그인 입니다.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, Enrollment.class);
            intent.putExtra("userProfile", userProfile);
            intent.putExtra("swuId", loginId);
            startActivity(intent);
            finish();

        }else if(loginId == null && loginPwd == null){
            //로그인 기록 없으면 메인엑티비티 보여줌
            loginBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    downProfile profileTask = new downProfile();
                    profileTask.execute(null, null);

                   }
            });
        }
    }

    /*인증 프로필 만들기*/
    class downProfile extends AsyncTask<String,String,String> {

        @Override
        protected String doInBackground(String... strings) {
            return createProfile();
            //return null;
        }

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(MainActivity.this);
            progress.show();
            progress.setMessage("사용자 확인 중 입니다.");
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i("log",result);
            parserProfile(result);

            if(userProfile != null) {
                editor = setting.edit();
                editor.putString("swuId", swuId.getText().toString());
                editor.putString("swuPwd", swuPwd.getText().toString());
                editor.putString("userProfile", userProfile);
                editor.commit();

                Toast.makeText(MainActivity.this, swuId.getText().toString()+"님의 음성등록페이지로 이동합니다.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, Enrollment.class);
                intent.putExtra("userProfile", userProfile);
                intent.putExtra("swuId", loginId);
                startActivity(intent);
                finish();
            }else{
                Toast.makeText(MainActivity.this, "[로그인 실패]\n인터넷을 연결하세요.", Toast.LENGTH_SHORT).show();

            }

            progress.dismiss();

            //super.onPostExecute(s);
        }
    }

    //create user profile using azure api
    private String createProfile(){
        String urlString = "https://westus.api.cognitive.microsoft.com/spid/v1.0/verificationProfiles";

        try {

            URL url = new URL(urlString);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            con.setRequestMethod("POST");
            con.setRequestProperty("Host", HOST);
            con.setRequestProperty("Content-Type", CON_TYPE );
            con.setRequestProperty("Ocp-Apim-Subscription-Key", API_KEY);

            con.setDoOutput(true);
            con.setDoInput(true);

            //Create JSONObject here
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("locale","en-us");
            OutputStreamWriter out = new   OutputStreamWriter(con.getOutputStream());
            out.write(jsonParam.toString());
            out.close();

            int responseCode = con.getResponseCode();

            BufferedReader br;
            if (responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            }else{
                br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            }

            String inputLine;
            StringBuffer response = new StringBuffer();
            while((inputLine = br.readLine())!=null){
                response.append(inputLine);
            }
            br.close();
            return response.toString();

        } catch (Exception e) {
            return e.toString();
        }
    }

    public void parserProfile(String result){
        try{
            userProfile = new JSONObject(result).getString("verificationProfileId");
        }catch (Exception e) { }

    }

    /*킷켓 이상부터 외부저장소 퍼미션 동적 추가
     * http://kylblog.tistory.com/12
     * */
    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 마시멜로우 버전과 같거나 이상이라면
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                /*
                if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "외부 저장소 사용을 위해 읽기/쓰기 필요", Toast.LENGTH_SHORT).show();
                }*/

                requestPermissions(new String[]
                                {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO},
                        3);//마지막 인자는 체크해야될 권한 갯수

                //Toast.makeText(MainActivity.this, "[ 권한설정 완료 ]앱을 완전히 종료 후 다시 시작하십시오.", Toast.LENGTH_SHORT).show();

                finish();

            }
        }
    }


}
