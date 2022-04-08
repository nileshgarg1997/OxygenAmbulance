package com.app.totoambulance;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void customerBtn(View view) {
        Intent i = new Intent(getApplicationContext(), UserLoginActivity.class);
        startActivity(i);
    }

    public void driverBtn(View view) {
        Intent i = new Intent(getApplicationContext(), DriverLoginActivity.class);
        startActivity(i);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.share_btn,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.shareBtn:
                Intent i=new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(i.EXTRA_SUBJECT,"Share");
                String shareMessage="https://play.google.com/store/apps/details?id="+BuildConfig.APPLICATION_ID+"\n\n";
                i.putExtra(i.EXTRA_TEXT,shareMessage);
                Intent chooser=Intent.createChooser(i,"Share the App using...");
                startActivity(chooser);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
