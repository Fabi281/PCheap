package de.dhbw.project.pcheap.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.dhbw.project.pcheap.R;
import de.dhbw.project.pcheap.pojo.DownloadImageTask;
import de.dhbw.project.pcheap.pojo.Item;

public class Details extends AppCompatActivity {

    SwipeListener swipeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        Item i = getIntent().getParcelableExtra("item");
        swipeListener = new SwipeListener(findViewById(R.id.DetailLayout));

        TextView textView;

        textView = findViewById(R.id.name);
        textView.setText(i.getName());

        textView = findViewById(R.id.price);
        textView.setText(Double.toString(i.getPrice()));

        textView = findViewById(R.id.description);
        textView.setText(i.getDescription());

        textView = findViewById(R.id.platform);
        textView.setText(i.getPlatform());

        textView = findViewById(R.id.url);
        textView.setText(i.getSiteUrl());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(new DownloadImageTask(findViewById(R.id.pic), i.getImageUrl()));

        setUpGraph(i);
    }

    private void setUpGraph(Item i){
        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(i.getHistory());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (jsonArray == null) {
            Toast.makeText(this, "No history available", Toast.LENGTH_SHORT).show();
            return;
        }

        GraphView graphView = findViewById(R.id.graph);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();

        long minDate = Long.MAX_VALUE;
        long maxDate = 0;
        double minPrice = Double.MAX_VALUE;
        double maxPrice = 0;
        double growth = 0;
        for (int j = 0; j < jsonArray.length(); j++) {
            DataPoint dp = null;
            try {
                JSONObject jsonObject = jsonArray.getJSONObject(j);
                Date date = new Date(jsonObject.getLong("timestamp")*1000L);
                double price = jsonObject.getDouble("price");
                if (j == jsonArray.length()-1)
                    growth = jsonObject.getDouble("growth");
                minDate = Math.min(minDate, date.getTime());
                maxDate = Math.max(maxDate, date.getTime());
                minPrice = Math.min(minPrice, price);
                maxPrice = Math.max(maxPrice, price);
                dp = new DataPoint(date, price);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            series.appendData(dp, true, jsonArray.length()+1);
        }

        series.setDrawDataPoints(true);
        series.setDataPointsRadius(10);
        series.setThickness(8);

        // set viewport to contain everything
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setYAxisBoundsManual(true);
        double xMargin = (maxDate-minDate)/5.;
        double yMargin = (maxPrice-minPrice)/5.;
        graphView.getViewport().setMinX(minDate-xMargin);
        graphView.getViewport().setMaxX(maxDate+xMargin);
        graphView.getViewport().setMinY(minPrice-yMargin);
        graphView.getViewport().setMaxY(maxPrice+yMargin);

        if (growth < 0)
            series.setColor(Color.GREEN);
        else if (growth > 0)
            series.setColor(Color.RED);
        else
            series.setColor(Color.YELLOW);


        graphView.addSeries(series);

        graphView.getGridLabelRenderer().setHumanRounding(false, true);
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
        graphView.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(this, sdf));
        graphView.getGridLabelRenderer().setNumHorizontalLabels(3);



        ImageView trendIndicator = findViewById(R.id.graph_trend_img);
        if (growth < 0)
            trendIndicator.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.arrow_down));
        else if (growth > 0)
            trendIndicator.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.arrow_up));
        else
            trendIndicator.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.arrow_flat));

    }

    private class SwipeListener implements View.OnTouchListener {

        GestureDetector gestureDetector;
        ArrayList<Item> itemList = getIntent().getParcelableArrayListExtra("itemList");
        int position = getIntent().getIntExtra("position", 0);

        SwipeListener(View v){
            int threshold = 100;
            int velocity_threshold = 100;

            GestureDetector.SimpleOnGestureListener listener =
                    new GestureDetector.SimpleOnGestureListener(){
                        @Override
                        public boolean onDown(MotionEvent e){
                          return true;
                        }

                        @Override
                        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                            float xDiff = e2.getX() - e1.getX();
                            try{
                                if (Math.abs(xDiff) > threshold && Math.abs(velocityX) > velocity_threshold){
                                    if(xDiff > 0){
                                        if(position - 1 >= 0){
                                            Intent in = new Intent(v.getContext(), Details.class);
                                            in.putExtra("item", itemList.get(position - 1));
                                            in.putParcelableArrayListExtra("itemList", itemList);
                                            in.putExtra("position", position - 1);
                                            v.getContext().startActivity(in);
                                            return true;
                                        }
                                    }else{
                                        if(position + 1 < itemList.size()){
                                            Intent in = new Intent(v.getContext(), Details.class);
                                            in.putExtra("item", itemList.get(position + 1));
                                            in.putParcelableArrayListExtra("itemList", itemList);
                                            in.putExtra("position", position + 1);
                                            v.getContext().startActivity(in);
                                            return true;
                                        }
                                    }
                                }
                            }catch(Exception e){
                                e.printStackTrace();
                            }
                            return false;
                        }
                    };
            gestureDetector = new GestureDetector(v.getContext(), listener);
            v.setOnTouchListener(this);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }
    }
}