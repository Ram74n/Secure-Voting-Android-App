package com.example.secureappvoting.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.secureappvoting.R;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultsActivity extends AppCompatActivity {

    private Spinner spinnerResultsPolls;
    private PieChart pieChart;
    private TextView tvResultsList;

    private FirebaseFirestore firestore;
    private ListenerRegistration voteListener;

    private final ArrayList<String> pollTitles = new ArrayList<>();
    private final ArrayList<String> pollIds = new ArrayList<>();

    // 🎓 University of Bradford themed colours
    private final int[] BRADFORD_COLORS = new int[]{
            Color.parseColor("#003A8F"), // Bradford Blue
            Color.parseColor("#F5B700"), // Gold
            Color.parseColor("#002B6B"),
            Color.parseColor("#6B6B6B")
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        spinnerResultsPolls = findViewById(R.id.spinnerResultsPolls);
        pieChart = findViewById(R.id.pieChart);
        tvResultsList = findViewById(R.id.tvResultsList);

        firestore = FirebaseFirestore.getInstance();

        setupChart();
        loadPolls();

        spinnerResultsPolls.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent,
                                               View view, int position, long id) {

                        if (position < pollIds.size()) {
                            listenForResults(pollIds.get(position));
                        }
                    }

                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                });
    }

    // ---------------- CHART CONFIG ----------------
    private void setupChart() {
        pieChart.setUsePercentValues(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleRadius(50f);
        pieChart.setCenterText("Poll Results");
        pieChart.setCenterTextSize(16f);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setNoDataText("Select a poll to view results");
    }

    // ---------------- LOAD POLLS ----------------
    private void loadPolls() {

        firestore.collection("polls")
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
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            pollTitles
                    );
                    adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                    );

                    spinnerResultsPolls.setAdapter(adapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load polls",
                                Toast.LENGTH_SHORT).show()
                );
    }

    // ---------------- REAL-TIME RESULTS ----------------
    private void listenForResults(String pollId) {

        if (voteListener != null) {
            voteListener.remove();
        }

        firestore.collection("polls")
                .document(pollId)
                .get()
                .addOnSuccessListener(pollDoc -> {

                    if (!pollDoc.exists()) return;

                    List<String> options =
                            (List<String>) pollDoc.get("options");

                    if (options == null || options.isEmpty()) {
                        tvResultsList.setText("No options available");
                        pieChart.clear();
                        return;
                    }

                    Map<String, Integer> voteCount = new HashMap<>();
                    for (String option : options) {
                        voteCount.put(option, 0);
                    }

                    voteListener = firestore.collection("votes")
                            .whereEqualTo("pollId", pollId)
                            .addSnapshotListener((snapshot, error) -> {

                                if (snapshot == null || error != null) return;

                                // Reset counts
                                for (String key : voteCount.keySet()) {
                                    voteCount.put(key, 0);
                                }

                                // Count votes
                                for (DocumentSnapshot vote : snapshot) {
                                    String selectedOption = vote.getString("option");
                                    if (selectedOption != null &&
                                            voteCount.containsKey(selectedOption)) {
                                        voteCount.put(
                                                selectedOption,
                                                voteCount.get(selectedOption) + 1
                                        );
                                    }
                                }

                                displayResults(voteCount);
                            });
                });
    }

    // ---------------- DISPLAY RESULTS ----------------
    private void displayResults(Map<String, Integer> voteCount) {

        ArrayList<PieEntry> entries = new ArrayList<>();
        StringBuilder resultText = new StringBuilder();

        boolean hasVotes = false;

        for (Map.Entry<String, Integer> entry : voteCount.entrySet()) {

            entries.add(new PieEntry(entry.getValue(), entry.getKey()));

            resultText.append("• ")
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append(" votes\n");

            if (entry.getValue() > 0) {
                hasVotes = true;
            }
        }

        tvResultsList.setText(resultText.toString());

        if (!hasVotes) {
            pieChart.clear();
            pieChart.setNoDataText("No votes have been cast yet");
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "Votes");
        dataSet.setColors(BRADFORD_COLORS);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.animateY(1200);
        pieChart.invalidate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voteListener != null) {
            voteListener.remove();
        }
    }
}
