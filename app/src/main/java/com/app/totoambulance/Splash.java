package com.app.totoambulance;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class Splash extends AppCompatActivity {

    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        getSupportActionBar().hide();
        handler=new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i=new Intent(Splash.this, WelcomeActivity.class);
                startActivity(i);
                finish();
            }
        },3000);


//        Thread thread = new Thread()
//        {
//            @Override
//            public void run() {                           //   Another method for showing splash screen
//                try {
//                    sleep(3000);
//                }
//                catch (Exception e){
//                    e.printStackTrace();
//                }
//                finally {
//                    Intent i=new Intent(getApplicationContext(),MainActivity.class);
//                    startActivity(i);
//                    finish();
//                }
//            }
//        };
//        thread.start();
    }
}