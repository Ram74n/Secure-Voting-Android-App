package com.example.secureappvoting.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.example.secureappvoting.R;

public class UserDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnVote, btnViewResults, btnLogout;

    private String userEmail;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        tvWelcome = findViewById(R.id.tvWelcome);
        btnVote = findViewById(R.id.btnVote);
        btnViewResults = findViewById(R.id.btnViewResults);
        btnLogout = findViewById(R.id.btnLogout);

        userEmail = getIntent().getStringExtra("email");
        userRole = getIntent().getStringExtra("role");

        tvWelcome.setText("Welcome, " + userEmail);

        btnVote.setOnClickListener(v -> {
            Intent i = new Intent(this, VoteActivity.class);
            i.putExtra("email", userEmail);
            i.putExtra("role", userRole);
            startActivity(i);
        });

        btnViewResults.setOnClickListener(v ->
                startActivity(new Intent(this, ResultsActivity.class))
        );

        btnLogout.setOnClickListener(v -> {
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });
    }
}
