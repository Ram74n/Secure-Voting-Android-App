package com.example.secureappvoting.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.secureappvoting.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvActivePolls, tvVotesCast;
    private Button btnCreatePoll, btnViewPolls, btnViewResults, btnLogout, btnClosePoll;

    private String userEmail;
    private boolean isAdmin = false;

    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // ----- UI -----
        tvWelcome = findViewById(R.id.tvWelcome);
        tvActivePolls = findViewById(R.id.tvActivePolls);
        tvVotesCast = findViewById(R.id.tvVotesCast);

        btnCreatePoll = findViewById(R.id.btnCreatePoll);
        btnViewPolls = findViewById(R.id.btnViewPolls);
        btnViewResults = findViewById(R.id.btnViewResults);
        btnLogout = findViewById(R.id.btnLogout);
        btnClosePoll = findViewById(R.id.btnClosePoll);

        firestore = FirebaseFirestore.getInstance();

        // ----- USER INFO -----
        userEmail = getIntent().getStringExtra("email");
        if (userEmail == null) userEmail = "User";

        tvWelcome.setText("Welcome, " + userEmail);

        // ----- LOAD DATA -----
        loadUserRole();
        loadDashboardStats();

        // ----- BUTTON ACTIONS -----
        btnViewPolls.setOnClickListener(v -> {
            Intent intent = new Intent(this, VoteActivity.class);
            intent.putExtra("email", userEmail);
            startActivity(intent);
        });

        btnViewResults.setOnClickListener(v ->
                startActivity(new Intent(this, ResultsActivity.class))
        );

        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    // ================= LOAD USER ROLE =================
    private void loadUserRole() {

        firestore.collection("users")
                .document(userEmail)
                .get()
                .addOnSuccessListener(doc -> {

                    if (doc.exists()) {
                        String role = doc.getString("role");
                        isAdmin = "admin".equalsIgnoreCase(role);
                    }

                    applyRolePermissions();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Failed to load user role",
                            Toast.LENGTH_SHORT).show();
                    applyRolePermissions();
                });
    }

    // ================= APPLY ROLE PERMISSIONS =================
    private void applyRolePermissions() {

        if (!isAdmin) {
            btnCreatePoll.setEnabled(false);
            btnCreatePoll.setAlpha(0.4f);

            btnClosePoll.setEnabled(false);
            btnClosePoll.setAlpha(0.4f);
        }

        btnCreatePoll.setOnClickListener(v -> {
            if (!isAdmin) {
                Toast.makeText(this,
                        "Only administrators can create polls.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(this, CreatePollActivity.class));
        });

        btnClosePoll.setOnClickListener(v -> {
            if (isAdmin) {
                showClosePollDialog();
            }
        });
    }

    // ================= DASHBOARD STATS =================
    private void loadDashboardStats() {

        firestore.collection("polls")
                .whereEqualTo("isOpen", true)
                .get()
                .addOnSuccessListener(snapshot ->
                        tvActivePolls.setText(String.valueOf(snapshot.size()))
                );

        firestore.collection("votes")
                .get()
                .addOnSuccessListener(snapshot ->
                        tvVotesCast.setText(String.valueOf(snapshot.size()))
                );
    }

    // ================= CLOSE POLL DIALOG =================
    private void showClosePollDialog() {

        firestore.collection("polls")
                .whereEqualTo("isOpen", true)
                .get()
                .addOnSuccessListener(snapshot -> {

                    ArrayList<String> pollTitles = new ArrayList<>();
                    ArrayList<String> pollIds = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        pollTitles.add(doc.getString("question"));
                        pollIds.add(doc.getId());
                    }

                    if (pollTitles.isEmpty()) {
                        Toast.makeText(this,
                                "No open polls available",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Close a Poll");

                    builder.setItems(pollTitles.toArray(new String[0]),
                            (dialog, which) -> {

                                firestore.collection("polls")
                                        .document(pollIds.get(which))
                                        .update("isOpen", false)
                                        .addOnSuccessListener(unused ->
                                                Toast.makeText(this,
                                                        "Poll closed successfully",
                                                        Toast.LENGTH_SHORT).show()
                                        );
                            });

                    builder.show();
                });
    }
}
