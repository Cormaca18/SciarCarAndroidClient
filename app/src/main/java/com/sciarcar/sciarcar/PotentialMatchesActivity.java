package com.sciarcar.sciarcar;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PotentialMatchesActivity extends AppCompatActivity {

    ListView potentialMatchesListView;

    ArrayList<PotentialMatch> potentialMatches;

    PotentialMatchAdapter adapter;

    String phoneTripId = "";

    Timer checkMatch;
    Timer getPotentialMatches;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_potential_matches);

        potentialMatchesListView = findViewById(R.id.potentialMatchesListView);

        potentialMatches = new ArrayList<>();

        adapter = new PotentialMatchAdapter(potentialMatches, getBaseContext(), getIntent().getStringExtra("tripID"));

        potentialMatchesListView.setAdapter(adapter);


        final Activity that = this;

        checkMatch = new Timer();

        checkMatch.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                List<AbstractMap.SimpleEntry<String, String>> params = new ArrayList<>();
                params.add(new AbstractMap.SimpleEntry("trip_id", getIntent().getStringExtra("tripID")));

                new DataPoster(){

                    @Override
                    protected void onResponse(int status, JSONObject message){

                        JSONArray result = null;
                        String matchId = "";
                        String tripId1= "";
                        String tripId2 = "";
                        String meetingPlace = "";


                        try {
                            result = message.getJSONArray("result");
                            matchId = String.valueOf(result.getString(0));
                            tripId1 = String.valueOf(result.getString(1));
                            tripId2 = String.valueOf(result.getString(2));
                            meetingPlace = String.valueOf(result.getString(5));

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if(!matchId.equals("")){

                            Intent i = new Intent(getApplicationContext(), MatchActivity.class);
                            i.putExtra("phoneTripID", phoneTripId);
                            i.putExtra("matchID", matchId);
                            i.putExtra("tripID1", tripId1);
                            i.putExtra("tripID2", tripId2);
                            i.putExtra("meetingPlace", meetingPlace);
                            startActivity(i);

                        }

                    }


                }.sendPost("https://sciarcar.herokuapp.com/has_trip_matched", params);

            }

        }, 0, 5000);

        getPotentialMatches = new Timer();
        //refresh potential matches every 10 seconds
        getPotentialMatches.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                List<AbstractMap.SimpleEntry<String, String>> params = new ArrayList<>();
                params.add(new AbstractMap.SimpleEntry("trip_id", getIntent().getStringExtra("tripID")));

                new DataPoster(){

                    @Override
                    protected void onResponse(int status, JSONObject message){

                        potentialMatches.clear();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                            }
                        });


                        String result = "jsonError";

                        try {
                            result = message.getString("result");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if(result.equals("success")){

                            JSONArray trips = null;
                            try {
                                trips = message.getJSONArray("trips");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            if(trips != null){
                                //loop through trips returned by the API and update variables
                                for(int i = 0; i< trips.length(); i++){

                                    JSONObject trip = null;

                                    try {
                                        trip = trips.getJSONObject(i);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    if(trip !=null){

                                        String origin = "origin";
                                        String dest = "dest";
                                        String tripId = "tripid";
                                        String numSeats = "numseats";
                                        boolean checked = false;
                                        boolean circle = false;

                                        try {
                                            circle = trip.getBoolean("circle");
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }


                                        try {
                                            numSeats = trip.getString("num_seats");
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                        try {
                                            checked = trip.getBoolean("checked");
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }


                                        try {
                                            origin = trip.getString("origin");
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                        try {
                                            dest = trip.getString("dest");
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                        try {
                                            tripId = trip.getString("trip_id");
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                        Log.d("done", "updating list");
                                        potentialMatches.add(new PotentialMatch(origin, dest, numSeats, tripId, checked, circle));

                                        that.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                adapter.notifyDataSetChanged();

                                            }
                                        });

                                    }
                                }
                            }
                        }else{
                            Log.d("potential_matches", "FAILURE - CLOSING ACTIVITY");
                            finish();
                        }

                    }



                }.sendPost("https://sciarcar.herokuapp.com/get_potential_matches", params);

            }
        }, 0, 10000);


    }

    @Override
    protected void onPause(){
        super.onPause();

        checkMatch.cancel();
        checkMatch = null;
        getPotentialMatches.cancel();
        getPotentialMatches = null;
    }
}
