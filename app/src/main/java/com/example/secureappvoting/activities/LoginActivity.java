package com.example.secureappvoting.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.secureappvoting.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGoToRegister;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoToRegister = findViewById(R.id.btnGoToRegister);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        btnLogin.setOnClickListener(v -> attemptLogin());
        btnGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }

    private void attemptLogin() {

        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this,
                    "Please enter both email and password",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {

                    final String role = email.contains("@admin") ? "admin" : "user";

                    firestore.collection("users")
                            .document(email)
                            .get()
                            .addOnSuccessListener(doc -> {

                                Map<String, Object> userData = new HashMap<>();
                                userData.put("email", email);
                                userData.put("role", role);

                                if (doc.exists()) {
                                    if (doc.getBoolean("hasVoted") != null)
                                        userData.put("hasVoted", doc.getBoolean("hasVoted"));
                                    if (doc.getString("votedPollId") != null)
                                        userData.put("votedPollId", doc.getString("votedPollId"));
                                } else {
                                    userData.put("hasVoted", false);
                                }

                                firestore.collection("users")
                                        .document(email)
                                        .set(userData)
                                        .addOnSuccessListener(unused -> {

                                            Intent i;
                                            if ("admin".equals(role)) {
                                                i = new Intent(this, DashboardActivity.class);
                                            } else {
                                                i = new Intent(this, VoteActivity.class);
                                            }

                                            i.putExtra("email", email);
                                            i.putExtra("role", role);
                                            startActivity(i);
                                            finish();
                                        });
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Login failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }
}
