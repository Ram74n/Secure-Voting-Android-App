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
    private TextView txtOptions, txtPollStatus, tvDeadlineStatus;
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
        tvDeadlineStatus = findViewById(R.id.tvDeadlineStatus);
        radioGroupOptions = findViewById(R.id.radioGroupOptions);
        btnSubmitVote = findViewById(R.id.btnSubmitVote);
        cardOptions = findViewById(R.id.cardOptions);

        firestore = FirebaseFirestore.getInstance();

        String roleExtra = getIntent().getStringExtra("role");
        if (roleExtra != null) userRole = roleExtra;

        resetUI();
        loadPolls();

        spinnerPolls.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent,
                                               View view, int position, long id) {

                        if (position < pollIds.size()) {
                            resetUI();
                            selectedPollId = pollIds.get(position);
                            checkPollStatusAndLoad(selectedPollId);
                        }
                    }

                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                });

        btnSubmitVote.setOnClickListener(v -> submitVote());
    }

    // ================= UI RESET =================
    private void resetUI() {
        txtOptions.setVisibility(View.GONE);
        txtPollStatus.setVisibility(View.GONE);
        tvDeadlineStatus.setVisibility(View.GONE);

        txtPollStatus.setText("");
        radioGroupOptions.removeAllViews();

        cardOptions.setVisibility(View.GONE);
        btnSubmitVote.setVisibility(View.GONE);
        btnSubmitVote.setEnabled(true);
        btnSubmitVote.setAlpha(1f);
    }

    // ================= LOAD POLLS =================
    private void loadPolls() {
        firestore.collection("polls")
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
                    adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                    );
                    spinnerPolls.setAdapter(adapter);
                });
    }

    // ================= TRAFFIC LIGHT LOGIC =================
    private void checkPollStatusAndLoad(String pollId) {

        firestore.collection("polls")
                .document(pollId)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) return;

                    Long deadlineAt = doc.getLong("deadlineAt");
                    boolean isOpen = Boolean.TRUE.equals(doc.getBoolean("isOpen"));
                    long now = System.currentTimeMillis();

                    tvDeadlineStatus.setVisibility(View.VISIBLE);

                    // 🔴 DEADLINE PASSED
                    if (deadlineAt != null && now >= deadlineAt) {

                        firestore.collection("polls")
                                .document(pollId)
                                .update("isOpen", false);

                        tvDeadlineStatus.setText("🔴 Voting closed");
                        tvDeadlineStatus.setTextColor(
                                getResources().getColor(android.R.color.holo_red_dark)
                        );

                        txtPollStatus.setText("This poll is closed");
                        txtPollStatus.setVisibility(View.VISIBLE);
                        return;
                    }

                    // 🟡 CLOSING SOON (last 30 minutes)
                    if (deadlineAt != null && deadlineAt - now <= 30 * 60 * 1000) {

                        tvDeadlineStatus.setText("🟡 Closing soon");
                        tvDeadlineStatus.setTextColor(
                                getResources().getColor(android.R.color.holo_orange_dark)
                        );
                    }

                    // 🟢 OPEN
                    if (deadlineAt == null || deadlineAt - now > 30 * 60 * 1000) {

                        tvDeadlineStatus.setText("🟢 Voting open");
                        tvDeadlineStatus.setTextColor(
                                getResources().getColor(android.R.color.holo_green_dark)
                        );
                    }

                    if (!isOpen) {
                        txtPollStatus.setText("Voting is closed");
                        txtPollStatus.setVisibility(View.VISIBLE);
                        return;
                    }

                    loadOptions(pollId);
                });
    }

    // ================= LOAD OPTIONS =================
    private void loadOptions(String pollId) {

        firestore.collection("polls")
                .document(pollId)
                .get()
                .addOnSuccessListener(doc -> {

                    List<String> options = (List<String>) doc.get("options");
                    if (options == null || options.isEmpty()) return;

                    txtOptions.setVisibility(View.VISIBLE);
                    cardOptions.setVisibility(View.VISIBLE);
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

    // ================= SUBMIT VOTE =================
    private void submitVote() {

        int selectedId = radioGroupOptions.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this,
                    "Please select an option",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selectedRadio = findViewById(selectedId);
        String selectedOption = selectedRadio.getText().toString();

        firestore.collection("polls")
                .document(selectedPollId)
                .update("votes." + selectedOption, FieldValue.increment(1))
                .addOnSuccessListener(unused -> {

                    if (!"admin".equals(userRole)) {
                        btnSubmitVote.setEnabled(false);
                        btnSubmitVote.setAlpha(0.4f);
                        txtPollStatus.setText("You have already voted");
                        txtPollStatus.setVisibility(View.VISIBLE);
                    }
                });
    }
}
