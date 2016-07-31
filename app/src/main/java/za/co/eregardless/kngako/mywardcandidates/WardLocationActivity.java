package za.co.eregardless.kngako.mywardcandidates;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONArray;
import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WardLocationActivity extends FragmentActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final int REQUEST_PLACE_PICKER = 1;
    private static final String TAG = "WardLocationActivity";

    private FloatingActionButton pickPlace;
    private TextView locationText;
    private LinearLayout resultView;
    private LinearLayout loadingContainer;

    private Boolean actionPerformded = false;

    private GoogleApiClient mGoogleApiClient;

    private LatLngBounds likelyPlaceBounds;
    private String likelyPlaceAddress;

    private final int MAX_AUTO_COMPLETE_LENGTH = 20; // PlaceAutocomplete doesn't find already available things too easy...

    private OkHttpClient client = new OkHttpClient();
    private final String wardURL = "http://wards.code4sa.org/?address=";
    private final String candidateURL = "http://wardcandidates.code4sa.org/?address=";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ward_location);

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        resultView = (LinearLayout) findViewById(R.id.result_list);
        locationText = (TextView) findViewById(R.id.location_text);
        pickPlace = (FloatingActionButton) findViewById(R.id.pick_location);
        loadingContainer = (LinearLayout) findViewById(R.id.loading_container);


            init();
    }

    public void init() {
        if (!((LocationManager) getSystemService(Context.LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGPSPrompt();
        }

        PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi
                .getCurrentPlace(mGoogleApiClient, null);

        result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
            @Override
            public void onResult(PlaceLikelihoodBuffer likelyPlaces) {
                if(likelyPlaces.getCount() > 0) {
                    likelyPlaceBounds = likelyPlaces.get(0).getPlace().getViewport();
                    likelyPlaceAddress = likelyPlaces.get(0).getPlace().getAddress().toString();
                    Toast.makeText(WardLocationActivity.this, "Appromixate location at: " + likelyPlaceAddress, Toast.LENGTH_LONG).show();
//                    Snackbar.make(findViewById(R.id.container), "Appromixate location at: " + likelyPlaceAddress, Snackbar.LENGTH_LONG).show();
                    Log.i(TAG, String.format("Place '%s' has likelihood: %g",
                            likelyPlaces.get(0).getPlace().getName(),
                            likelyPlaces.get(0).getLikelihood()));
                }

                if(!actionPerformded)
                    loadingContainer.setVisibility(View.GONE);

                likelyPlaces.release();
            }
        });

        pickPlace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Construct an intent for the place picker
                try {
                    PlaceAutocomplete.IntentBuilder builder = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN);

                    builder.setFilter(new AutocompleteFilter.Builder()
                                            .setTypeFilter(AutocompleteFilter.TYPE_FILTER_ADDRESS)
                                            .build());

                    if(likelyPlaceBounds != null ){
                        builder.setBoundsBias(likelyPlaceBounds);
                    }
                    if(likelyPlaceAddress != null ){
                        if(likelyPlaceAddress.length() > MAX_AUTO_COMPLETE_LENGTH)
                            likelyPlaceAddress = likelyPlaceAddress.substring(0, MAX_AUTO_COMPLETE_LENGTH -1);
                        builder.zzkv(likelyPlaceAddress);
                    }
                    Intent intent = builder.build(WardLocationActivity.this);

                    startActivityForResult(intent, REQUEST_PLACE_PICKER);

                } catch (GooglePlayServicesRepairableException e) {
                    // TODO: Handle exception...
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    // TODO: Handle exception...
                    e.printStackTrace();
                }
            }
        });
    }


    private void showGPSPrompt() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("CSaw needs GPS to set your Lat Lng coordinates.");
// Add the buttons
        builder.setPositiveButton("Enable GPS", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 0); // TODO: What to do with result..
            }
        });
        builder.setNegativeButton("No Thanks", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                dialog.dismiss();
            }
        });
// Set other dialog properties

// Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {

        if (requestCode == REQUEST_PLACE_PICKER) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);

                locationText.setText(place.getName() + ": " + place.getAddress());
                locationText.setTextColor(Color.BLACK);

                try {
                    getCandidates(URLEncoder.encode(place.getAddress().toString(), "UTF-8"));

                } catch (Exception e) {
                    Toast.makeText(this, "Cant process location...", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                // TODO: Handle the error.
                Log.e("Kgothatso", status.getStatusMessage());
                locationText.setText(status.getStatusMessage());
                locationText.setTextColor(Color.RED);
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // TODO: Handle failure code...
        Log.d(TAG, "Connection result failed... " + connectionResult.getErrorMessage());
    }


    private void getCandidates(final String address) {
        Log.d(TAG, "Accessing: " + candidateURL + address);
        actionPerformded = true;

        if(loadingContainer.getVisibility() != View.VISIBLE)
            loadingContainer.setVisibility(View.VISIBLE);

        resultView.removeAllViews();

        Request request = new Request.Builder()
                .url(candidateURL + address)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                // TODO: Give advice on internet settings...
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        actionPerformded = false;
                        loadingContainer.setVisibility(View.GONE);

                        TextView error = (TextView) getLayoutInflater().inflate(android.R.layout.simple_list_item_1, resultView, false);
                        error.setText("Internet connection is flawed... check data or proxy settings...");

                        resultView.addView(error);
                    }
                });

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String contents = response.body().string();
                Log.d(TAG, "Retrieved: " + contents);

                Document doc  = Jsoup.parse(contents); // I would've prefered getting json... but oh well...
                final Elements names = doc.getElementsByTag("li");
                Log.d(TAG, "Names: " + names.size());
                if(names.size() > 0){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            for(Element name: names){
                                if(names.indexOf(name) != 0)
                                    resultView.addView(getLayoutInflater().inflate(R.layout.seperator, resultView, false));

                                Log.d(TAG, "Result: " + name.html());
                                TextView resultName = (TextView) getLayoutInflater().inflate(android.R.layout.simple_list_item_1, resultView, false);
                                resultName.setText(Html.fromHtml(Html.fromHtml(name.html()).toString()));
                                resultName.setTextColor(Color.BLUE);

                                resultView.addView(resultName);

                            }
                        }
                    });

                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView text = (TextView) getLayoutInflater().inflate(android.R.layout.simple_list_item_1, resultView, false);
                            text.setText("Unfortanetly your ward isn't currently in the database :(");

                            resultView.addView(text);
                        }
                    });

                }
                getWardNumber(address);
            }
        });

    }

    private void getWardNumber(final String address) {
        Log.d(TAG, "Getting word number for: " + wardURL + address);

        actionPerformded = true;

        if(loadingContainer.getVisibility() != View.VISIBLE)
            loadingContainer.setVisibility(View.VISIBLE);

        Request request = new Request.Builder()
                .url(wardURL + address)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        actionPerformded = true;

                        if(loadingContainer.getVisibility() != View.VISIBLE)
                            loadingContainer.setVisibility(View.VISIBLE);

                        TextView text = (TextView) getLayoutInflater().inflate(android.R.layout.simple_list_item_1, resultView, false);
                        text.setText("Internet problems... check data or proxy... :(");

                        resultView.addView(text);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String contents = response.body().string();
                Log.d(TAG, "Retrieved: " + contents);

                JSONArray jsonArray  = null;
                try {
                    jsonArray = new JSONArray(contents);
                    Log.d(TAG, "Names: " + jsonArray.length());
                    if(jsonArray.length() > 0){
                        // TODO: Process
                        final String ward = jsonArray.getJSONObject(0).getString("ward");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView text = (TextView) getLayoutInflater().inflate(android.R.layout.simple_list_item_1, resultView, false);
                                text.setText("Detected Ward No: " + ward);
                                text.setGravity(Gravity.CENTER);
                                resultView.addView(text);
                            }
                        });

                    } else {
                        // TODO: tell user that none was found...
                        // Let them know their ward number...
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView text = (TextView) getLayoutInflater().inflate(android.R.layout.simple_list_item_1, resultView, false);
                                text.setText("No ward data for your location");
                                text.setGravity(Gravity.CENTER_HORIZONTAL);

                                resultView.addView(text);
                            }
                        });

                    }
                } catch (JSONException e) {
                    e.printStackTrace();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView text = (TextView) getLayoutInflater().inflate(android.R.layout.simple_list_item_1, resultView, false);
                            text.setText("Your ward is really hard to look up");

                            resultView.addView(text);
                        }
                    });
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        actionPerformded = false;

                        loadingContainer.setVisibility(View.GONE);
                    }
                });

            }
        });
    }
}
