package com.example.secureappvoting.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.secureappvoting.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnGoToRegister;

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

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this,
                        "Enter both email and password",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔐 Firebase Authentication
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {

                        // 🔎 Fetch user role from Firestore
                        firestore.collection("users")
                                .document(email)
                                .get()
                                .addOnSuccessListener(doc -> {

                                    boolean isAdmin = false;

                                    if (doc.exists()) {
                                        String role = doc.getString("role");
                                        isAdmin = "admin".equalsIgnoreCase(role);
                                    }

                                    Intent intent = new Intent(
                                            LoginActivity.this,
                                            DashboardActivity.class
                                    );
                                    intent.putExtra("email", email);
                                    intent.putExtra("isAdmin", isAdmin);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this,
                                                "Failed to load user role",
                                                Toast.LENGTH_SHORT).show()
                                );
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    "Login Failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show()
                    );
        });

        btnGoToRegister.setOnClickListener(v -> {
            startActivity(
                    new Intent(LoginActivity.this, RegisterActivity.class)
            );
        });
    }
}
