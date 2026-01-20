package com.example.secureappvoting.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.secureappvoting.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoteActivity extends AppCompatActivity {

    private Spinner spinnerPolls;
    private TextView txtOptions;
    private RadioGroup radioGroupOptions;
    private Button btnSubmitVote;

    private FirebaseFirestore firestore;

    private final ArrayList<String> pollTitles = new ArrayList<>();
    private final ArrayList<String> pollIds = new ArrayList<>();

    private String userEmail;
    private boolean hasVoted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vote);

        spinnerPolls = findViewById(R.id.spinnerPolls);
        txtOptions = findViewById(R.id.txtOptions);
        radioGroupOptions = findViewById(R.id.radioGroupOptions);
        btnSubmitVote = findViewById(R.id.btnSubmitVote);

        firestore = FirebaseFirestore.getInstance();

        userEmail = getIntent().getStringExtra("email");
        if (userEmail == null) userEmail = "anonymous";

        hideVoteUI();
        loadPolls();

        spinnerPolls.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent,
                                               View view,
                                               int position,
                                               long id) {

                        if (position < pollIds.size()) {
                            hideVoteUI();
                            hasVoted = false;

                            String pollId = pollIds.get(position);
                            loadOptions(pollId);
                            checkIfUserVoted(pollId);
                        }
                    }

                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                });

        btnSubmitVote.setOnClickListener(v -> {
            if (!hasVoted) {
                submitVote();
            }
        });
    }

    // ---------------- UI HELPERS ----------------
    private void hideVoteUI() {
        radioGroupOptions.removeAllViews();
        radioGroupOptions.setVisibility(View.GONE);
        txtOptions.setVisibility(View.GONE);
        btnSubmitVote.setVisibility(View.GONE);
        btnSubmitVote.setEnabled(true);
        btnSubmitVote.setAlpha(1f);
    }

    private void showVoteUI() {
        txtOptions.setVisibility(View.VISIBLE);
        radioGroupOptions.setVisibility(View.VISIBLE);
        btnSubmitVote.setVisibility(View.VISIBLE);
    }

    // ---------------- LOAD POLLS ----------------
    private void loadPolls() {
        firestore.collection("polls")
                .whereEqualTo("isOpen", true)
                .get()
                .addOnSuccessListener(snapshot -> {

                    pollTitles.clear();
                    pollIds.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String question = doc.getString("question");
                        if (question != null) {
                            pollIds.add(doc.getId());
                            pollTitles.add(question);
                        }
                    }

                    if (pollTitles.isEmpty()) {
                        pollTitles.add("No polls available");
                        spinnerPolls.setEnabled(false);
                    } else {
                        spinnerPolls.setEnabled(true);
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
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load polls",
                                Toast.LENGTH_SHORT).show()
                );
    }

    // ---------------- LOAD OPTIONS ----------------
    private void loadOptions(String pollId) {
        firestore.collection("polls")
                .document(pollId)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        Toast.makeText(this,
                                "Poll not found",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> options = (List<String>) doc.get("options");

                    if (options == null || options.isEmpty()) {
                        txtOptions.setText("No options available");
                        txtOptions.setVisibility(View.VISIBLE);
                        return;
                    }

                    showVoteUI();
                    radioGroupOptions.removeAllViews();

                    for (String option : options) {
                        RadioButton rb = new RadioButton(this);
                        rb.setText(option);
                        rb.setId(View.generateViewId());
                        radioGroupOptions.addView(rb);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load options",
                                Toast.LENGTH_SHORT).show()
                );
    }

    // ---------------- CHECK DUPLICATE VOTE ----------------
    private void checkIfUserVoted(String pollId) {
        firestore.collection("votes")
                .whereEqualTo("pollId", pollId)
                .whereEqualTo("userEmail", userEmail)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!snapshot.isEmpty()) {
                        hasVoted = true;
                        btnSubmitVote.setEnabled(false);
                        btnSubmitVote.setAlpha(0.4f);
                        txtOptions.setText("You have already voted in this poll");
                        txtOptions.setVisibility(View.VISIBLE);
                    }
                });
    }

    // ---------------- SUBMIT VOTE ----------------
    private void submitVote() {

        btnSubmitVote.setEnabled(false); // 🔒 immediate lock
        btnSubmitVote.setAlpha(0.4f);

        int selectedId = radioGroupOptions.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this,
                    "Please select an option",
                    Toast.LENGTH_SHORT).show();
            btnSubmitVote.setEnabled(true);
            btnSubmitVote.setAlpha(1f);
            return;
        }

        int position = spinnerPolls.getSelectedItemPosition();
        if (position >= pollIds.size()) return;

        String pollId = pollIds.get(position);
        String pollTitle = pollTitles.get(position);

        RadioButton selectedRadio = findViewById(selectedId);
        if (selectedRadio == null) return;

        String selectedOption = selectedRadio.getText().toString();

        Map<String, Object> voteData = new HashMap<>();
        voteData.put("pollId", pollId);
        voteData.put("pollTitle", pollTitle);
        voteData.put("option", selectedOption);
        voteData.put("userEmail", userEmail);
        voteData.put("timestamp", System.currentTimeMillis());

        firestore.collection("votes")
                .add(voteData)
                .addOnSuccessListener(doc -> {
                    hasVoted = true;
                    radioGroupOptions.clearCheck();

                    Toast.makeText(this,
                            "Vote submitted successfully!",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Failed to submit vote",
                            Toast.LENGTH_SHORT).show();
                    btnSubmitVote.setEnabled(true);
                    btnSubmitVote.setAlpha(1f);
                });
    }
}
