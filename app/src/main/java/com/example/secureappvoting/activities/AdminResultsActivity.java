package com.example.secureappvoting.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.secureappvoting.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Map;

public class AdminResultsActivity extends AppCompatActivity {

    private Spinner spinnerPolls;
    private TextView txtResults;
    private FirebaseFirestore firestore;

    private final ArrayList<String> pollTitles = new ArrayList<>();
    private final ArrayList<String> pollIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_results);

        spinnerPolls = findViewById(R.id.spinnerPolls);
        txtResults = findViewById(R.id.txtResults);

        firestore = FirebaseFirestore.getInstance();

        verifyAdminAndLoad();
    }

    // ================= ADMIN CHECK =================
    private void verifyAdminAndLoad() {
        String email = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail()
                : null;

        if (email == null) {
            finish();
            return;
        }

        firestore.collection("users")
                .document(email)
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");

                    if (!"admin".equals(role)) {
                        Toast.makeText(this,
                                "Access denied",
                                Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    loadPolls();
                });
    }

    // ================= LOAD POLLS =================
    private void loadPolls() {
        firestore.collection("polls")
                .get()
                .addOnSuccessListener(snapshot -> {

                    pollTitles.clear();
                    pollIds.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        pollIds.add(doc.getId());
                        pollTitles.add(doc.getString("question"));
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            pollTitles
                    );
                    adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                    );
                    spinnerPolls.setAdapter(adapter);

                    spinnerPolls.setOnItemSelectedListener(
                            new android.widget.AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(
                                        android.widget.AdapterView<?> parent,
                                        View view,
                                        int position,
                                        long id
                                ) {
                                    if (position < pollIds.size()) {
                                        loadResults(pollIds.get(position));
                                    }
                                }

                                @Override
                                public void onNothingSelected(
                                        android.widget.AdapterView<?> parent) {}
                            });
                });
    }

    // ================= LOAD RESULTS =================
    private void loadResults(String pollId) {
        firestore.collection("polls")
                .document(pollId)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) return;

                    Map<String, Long> votes =
                            (Map<String, Long>) doc.get("votes");

                    StringBuilder results = new StringBuilder();

                    if (votes == null || votes.isEmpty()) {
                        results.append("No votes yet.");
                    } else {
                        for (String option : votes.keySet()) {
                            results.append(option)
                                    .append(": ")
                                    .append(votes.get(option))
                                    .append(" votes\n");
                        }
                    }

                    txtResults.setText(results.toString());
                });
    }
}
