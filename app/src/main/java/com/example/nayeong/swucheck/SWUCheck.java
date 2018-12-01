package com.example.nayeong.swucheck;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class SWUCheck extends AppCompatActivity {
    static final String HOST = "westus.api.cognitive.microsoft.com";
    static final String API_KEY ="";

    //azure private user profile and this app user id
    String userProfile, loginId, enrollStatus;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.swucheck);

        Intent getIntent = getIntent();
        userProfile = getIntent.getStringExtra("userProfile");
        loginId = getIntent.getStringExtra("loginId");
        enrollStatus = getIntent.getStringExtra("enrollStatus");

    }
}
