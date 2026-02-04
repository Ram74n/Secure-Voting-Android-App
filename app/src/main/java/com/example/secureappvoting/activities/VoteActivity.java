package com.example.secureappvoting.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.secureappvoting.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class VoteActivity extends AppCompatActivity {

    private Spinner spinnerPolls;
    private TextView txtOptions, txtPollStatus;
    private RadioGroup radioGroupOptions;
    private MaterialButton btnSubmitVote;
    private MaterialCardView cardOptions;

    private FirebaseFirestore firestore;

    private final ArrayList<String> pollTitles = new ArrayList<>();
    private final ArrayList<String> pollIds = new ArrayList<>();

    private String userRole = "user";
    private String selectedPollId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vote);

        spinnerPolls = findViewById(R.id.spinnerPolls);
        txtOptions = findViewById(R.id.txtOptions);
        txtPollStatus = findViewById(R.id.txtPollStatus);
        radioGroupOptions = findViewById(R.id.radioGroupOptions);
        btnSubmitVote = findViewById(R.id.btnSubmitVote);
        cardOptions = findViewById(R.id.cardOptions);

        firestore = FirebaseFirestore.getInstance();

        String roleExtra = getIntent().getStringExtra("role");
        if (roleExtra != null) userRole = roleExtra;

        resetUI();
        loadPolls();

        spinnerPolls.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {

                if (position < pollIds.size()) {
                    resetUI();
                    selectedPollId = pollIds.get(position);
                    loadOptions(selectedPollId);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnSubmitVote.setOnClickListener(v -> submitVote());
    }

    private void resetUI() {
        txtOptions.setVisibility(View.GONE);
        txtPollStatus.setVisibility(View.GONE);
        txtPollStatus.setText("");

        cardOptions.setVisibility(View.GONE);
        radioGroupOptions.setVisibility(View.GONE);
        radioGroupOptions.removeAllViews();

        btnSubmitVote.setVisibility(View.GONE);
        btnSubmitVote.setEnabled(true);
        btnSubmitVote.setAlpha(1f);
    }

    private void loadPolls() {
        firestore.collection("polls")
                .whereEqualTo("isOpen", true)
                .get()
                .addOnSuccessListener(snapshot -> {

                    pollTitles.clear();
                    pollIds.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        pollTitles.add(doc.getString("question"));
                        pollIds.add(doc.getId());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            pollTitles
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerPolls.setAdapter(adapter);
                });
    }

    private void loadOptions(String pollId) {
        firestore.collection("polls")
                .document(pollId)
                .get()
                .addOnSuccessListener(doc -> {

                    List<String> options = (List<String>) doc.get("options");
                    if (options == null || options.isEmpty()) return;

                    txtOptions.setVisibility(View.VISIBLE);
                    cardOptions.setVisibility(View.VISIBLE);
                    radioGroupOptions.setVisibility(View.VISIBLE);
                    btnSubmitVote.setVisibility(View.VISIBLE);

                    radioGroupOptions.removeAllViews();

                    for (String option : options) {
                        RadioButton rb = new RadioButton(this);
                        rb.setText(option);
                        rb.setId(View.generateViewId());
                        radioGroupOptions.addView(rb);
                    }
                });
    }

    private void submitVote() {

        int selectedId = radioGroupOptions.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Please select an option", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selectedRadio = findViewById(selectedId);
        String selectedOption = selectedRadio.getText().toString();

        firestore.collection("polls")
                .document(selectedPollId)
                .update("votes." + selectedOption, FieldValue.increment(1))
                .addOnSuccessListener(unused -> {

                    // 👑 ADMIN: unlimited voting, no locks, no messages
                    if ("admin".equals(userRole)) {
                        btnSubmitVote.setEnabled(true);
                        btnSubmitVote.setAlpha(1f);
                        return;
                    }

                    // 👤 USER: one vote per poll (UI lock only)
                    btnSubmitVote.setEnabled(false);
                    btnSubmitVote.setAlpha(0.4f);
                    txtPollStatus.setText("You have already voted in this poll");
                    txtPollStatus.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Vote failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }
}
