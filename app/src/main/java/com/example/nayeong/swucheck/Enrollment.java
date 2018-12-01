package com.example.nayeong.swucheck;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;

public class Enrollment extends Activity {
    static final String HOST = "westus.api.cognitive.microsoft.com";
    static final String CON_TYPE = "application/json";
    static final String API_KEY ="58d7f66f03ae4e818c5bb1fcd3ee04b7";

    //azure private user profile
    String userProfile;

    ProgressDialog progress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.enrollment);

        Intent intent = getIntent();
        userProfile = intent.getStringExtra("userProfile");






    }
}
