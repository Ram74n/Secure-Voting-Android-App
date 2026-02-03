package com.example.secureappvoting.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.secureappvoting.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CreatePollActivity extends AppCompatActivity {

    private EditText etPollQuestion, etOption1, etOption2, etOption3;
    private Button btnCreatePoll;

    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_poll);

        etPollQuestion = findViewById(R.id.etPollQuestion);
        etOption1 = findViewById(R.id.etOption1);
        etOption2 = findViewById(R.id.etOption2);
        etOption3 = findViewById(R.id.etOption3);
        btnCreatePoll = findViewById(R.id.btnCreatePoll);

        firestore = FirebaseFirestore.getInstance();

        btnCreatePoll.setOnClickListener(v -> createPoll());
    }

    private void createPoll() {

        String question = etPollQuestion.getText().toString().trim();
        String op1 = etOption1.getText().toString().trim();
        String op2 = etOption2.getText().toString().trim();
        String op3 = etOption3.getText().toString().trim();

        if (question.isEmpty() || op1.isEmpty() || op2.isEmpty()) {
            Toast.makeText(this,
                    "Poll question and at least two options are required",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String> options = new ArrayList<>();
        options.add(op1);
        options.add(op2);
        if (!op3.isEmpty()) options.add(op3);

        // 🔥 AUTO CREATE VOTES MAP
        Map<String, Object> votes = new HashMap<>();
        for (String option : options) {
            votes.put(option, 0);
        }

        Map<String, Object> poll = new HashMap<>();
        poll.put("question", question);
        poll.put("options", options);
        poll.put("votes", votes);
        poll.put("isOpen", true);
        poll.put("createdAt", System.currentTimeMillis());

        firestore.collection("polls")
                .add(poll)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this,
                            "Poll published successfully",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }
}
