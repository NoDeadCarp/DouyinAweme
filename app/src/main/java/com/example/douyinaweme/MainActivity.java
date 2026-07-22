package com.example.douyinaweme;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.knownniu.douyinaweme.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        TextView tvVersion = findViewById(R.id.tvVersion);
        View statusDot = findViewById(R.id.statusDot);

        try {
            String ver = getPackageManager()
                    .getPackageInfo("com.ss.android.ugc.aweme", 0).versionName;
            tvVersion.setText("com.ss.android.ugc.aweme  v" + ver);
            tvVersion.setTextColor(0xFF4CAF50);
            statusDot.setBackgroundColor(0xFF4CAF50);
        } catch (Exception e) {
            tvVersion.setText("Not installed");
            tvVersion.setTextColor(0xFFF44336);
            statusDot.setBackgroundColor(0xFFF44336);
        }
    }
}
