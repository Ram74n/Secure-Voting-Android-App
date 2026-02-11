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
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResultsActivity extends AppCompatActivity {

    private Spinner spinnerResultsPolls;
    private PieChart pieChart;
    private BarChart barChart;
    private TextView tvResultsList, tvTotalVotes, tvWinningOption;

    private FirebaseFirestore firestore;
    private ListenerRegistration pollListener;

    private final ArrayList<String> pollTitles = new ArrayList<>();
    private final ArrayList<String> pollIds = new ArrayList<>();

    private final int[] BRADFORD_COLORS = new int[]{
            Color.parseColor("#003A8F"),
            Color.parseColor("#F5B700"),
            Color.parseColor("#002B6B"),
            Color.parseColor("#6B6B6B")
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        spinnerResultsPolls = findViewById(R.id.spinnerResultsPolls);
        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);
        tvResultsList = findViewById(R.id.tvResultsList);
        tvTotalVotes = findViewById(R.id.tvTotalVotes);
        tvWinningOption = findViewById(R.id.tvWinningOption);

        firestore = FirebaseFirestore.getInstance();

        setupPieChart();
        setupBarChart();
        loadPolls();

        spinnerResultsPolls.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent,
                                               View view, int position, long id) {
                        if (position < pollIds.size()) {
                            listenToPollResults(pollIds.get(position));
                        }
                    }
                    @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                });
    }

    // ---------------- PIE CHART ----------------
    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleRadius(50f);
        pieChart.setCenterText("Vote Share");
        pieChart.setCenterTextSize(16f);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setNoDataText("Select a poll to view results");

        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
    }

    // ---------------- BAR CHART ----------------
    private void setupBarChart() {
        barChart.getDescription().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setGranularity(1f);
        barChart.getXAxis().setGranularity(1f);
        barChart.setNoDataText("Select a poll to view results");
        barChart.getLegend().setEnabled(false);
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
                            pollTitles.add(question);
                            pollIds.add(doc.getId());
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
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerResultsPolls.setAdapter(adapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load polls", Toast.LENGTH_SHORT).show()
                );
    }

    // ---------------- LIVE RESULTS ----------------
    private void listenToPollResults(String pollId) {

        if (pollListener != null) pollListener.remove();

        pollListener = firestore.collection("polls")
                .document(pollId)
                .addSnapshotListener((doc, err) -> {

                    if (doc == null || err != null || !doc.exists()) return;

                    List<String> options = (List<String>) doc.get("options");
                    Map<String, Long> votesMap = (Map<String, Long>) doc.get("votes");

                    if (options == null || options.isEmpty()) return;

                    Map<String, Integer> voteCount = new LinkedHashMap<>();
                    for (String option : options) {
                        int v = votesMap != null && votesMap.get(option) != null
                                ? votesMap.get(option).intValue()
                                : 0;
                        voteCount.put(option, v);
                    }

                    renderAll(voteCount);
                });
    }

    // ---------------- RENDER EVERYTHING ----------------
    private void renderAll(Map<String, Integer> voteCount) {

        int totalVotes = 0;
        String winner = "-";
        int max = -1;

        for (Map.Entry<String, Integer> e : voteCount.entrySet()) {
            totalVotes += e.getValue();
            if (e.getValue() > max) {
                max = e.getValue();
                winner = e.getKey();
            }
        }

        tvTotalVotes.setText("Total Votes: " + totalVotes);
        tvWinningOption.setText("Leading Option: " + (max <= 0 ? "-" : winner + " (" + max + ")"));

        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, Integer> e : voteCount.entrySet()) {
            text.append("• ").append(e.getKey()).append(": ")
                    .append(e.getValue()).append(" votes\n");
        }
        tvResultsList.setText(text.toString());

        if (totalVotes == 0) {
            pieChart.clear();
            barChart.clear();
            pieChart.invalidate();
            barChart.invalidate();
            return;
        }

        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        for (Map.Entry<String, Integer> e : voteCount.entrySet()) {
            pieEntries.add(new PieEntry(e.getValue(), e.getKey()));
        }

        PieDataSet pieSet = new PieDataSet(pieEntries, "Vote Share");
        pieSet.setColors(BRADFORD_COLORS);
        pieSet.setValueTextColor(Color.WHITE);
        pieSet.setValueTextSize(14f);

        PieData pieData = new PieData(pieSet);
        pieData.setValueFormatter(new PercentFormatter(pieChart));
        pieChart.setData(pieData);

        ArrayList<BarEntry> barEntries = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Integer> e : voteCount.entrySet()) {
            barEntries.add(new BarEntry(i++, e.getValue()));
        }

        BarDataSet barSet = new BarDataSet(barEntries, "Votes");
        barSet.setColors(BRADFORD_COLORS);
        barSet.setValueTextSize(14f);

        BarData barData = new BarData(barSet);
        barChart.setData(barData);

        pieChart.animateY(1000);
        barChart.animateY(1000);

        pieChart.invalidate();
        barChart.invalidate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pollListener != null) pollListener.remove();
    }
}
