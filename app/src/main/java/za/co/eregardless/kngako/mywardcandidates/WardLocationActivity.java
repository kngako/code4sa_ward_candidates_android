package za.co.eregardless.kngako.mywardcandidates;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceFilter;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * TODO:
 * Add pictures...
 * Abraviate Party Names...
 */
public class WardLocationActivity extends FragmentActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final int REQUEST_PLACE_PICKER = 1;
    private final int INVITE_REQUEST_CODE = 2;
    private static final String TAG = "WardLocationActivity";

    private Button pickPlace;
    private TextView locationText;
    private LinearLayout candidateList;
    private LinearLayout loadingContainer;

    private View resultsContainer;
    private View demographicsContainer;

    private WebView results2014;
    private WebView results2011;

    private WebView ageDemographics;
    private WebView raceDemographics;

    private View waziMapIcon;

    private Boolean actionPerformded = false;

    private GoogleApiClient mGoogleApiClient;

    private LatLngBounds likelyPlaceBounds;
    private String likelyPlaceAddress;

    private final int MAX_AUTO_COMPLETE_LENGTH = 25; // PlaceAutocomplete doesn't find already available things too easy...

    private OkHttpClient client = new OkHttpClient();
    private final String wardURL = "http://mapit.code4sa.org/point/4326/%s,%s?generation=2&type=WD";
    private final String candidateURL = "http://41.185.29.119:5000/api/councillors?lat=%s&lon=%s";

    private final String waziMap2014 = "<iframe id=\"cr-embed-ward-%s-elections-national_2014-party_distribution\" class=\"census-reporter-embed\" src=\"https://wazimap.co.za/embed/iframe.html?geoID=ward-%s&chartDataID=elections-national_2014-party_distribution&dataYear=2014&chartType=histogram&chartHeight=200&chartQualifier=&chartTitle=Voters+by+party&initialSort=&statType=scaled-percentage\" frameborder=\"0\" height=\"300\" style=\"margin: 1em; max-width: 720px; float: right;\"></iframe>\n" +
            "<script src=\"https://wazimap.co.za/static/js/embed.chart.make.js\"></script>";
    private final String waziMap2011 = "<iframe id=\"cr-embed-ward-%s-elections-municipal_2011-party_distribution\" class=\"census-reporter-embed\" src=\"https://wazimap.co.za/embed/iframe.html?geoID=ward-%s&chartDataID=elections-municipal_2011-party_distribution&dataYear=2011&chartType=histogram&chartHeight=200&chartQualifier=&chartTitle=Voters+by+party&initialSort=&statType=scaled-percentage\" frameborder=\"0\" height=\"300\" style=\"margin: 1em; max-width: 720px;\"></iframe>\n" +
            "<script src=\"https://wazimap.co.za/static/js/embed.chart.make.js\"></script>";

    private final String waziMapAGE = "<iframe id=\"cr-embed-ward-%s-demographics-age_group_distribution\" class=\"census-reporter-embed\" src=\"https://wazimap.co.za/embed/iframe.html?geoID=ward-%s&chartDataID=demographics-age_group_distribution&dataYear=2011&chartType=histogram&chartHeight=200&chartQualifier=&chartTitle=Population+by+age+range&initialSort=&statType=scaled-percentage\" frameborder=\"0\" height=\"300\" style=\"margin: 1em; max-width: 720px;\"></iframe>\n" +
            "<script src=\"https://wazimap.co.za/static/js/embed.chart.make.js\"></script>";
    private final String waziMapRace = "<iframe id=\"cr-embed-ward-%s-demographics-population_group_distribution\" class=\"census-reporter-embed\" src=\"https://wazimap.co.za/embed/iframe.html?geoID=ward-%s&chartDataID=demographics-population_group_distribution&dataYear=2011&chartType=column&chartHeight=200&chartQualifier=&chartTitle=Population+group&initialSort=&statType=scaled-percentage\" frameborder=\"0\" height=\"300\" style=\"margin: 1em; max-width: 720px;\"></iframe>\n" +
            "<script src=\"https://wazimap.co.za/static/js/embed.chart.make.js\"></script>";

    private final String liveResultsURL = "http://elections.sabc.co.za/Results/GetAjaxData"; // TODO: Use this in app...

    private final String code4saURL = "http://codebridge.co.za/";
    private final String eregardlessURL = "http://eregardless.co.za/";
    private final String wazimapURL = "https://wazimap.co.za/";
    private final String wazimapWardURL = "https://wazimap.co.za/profiles/ward-%s-ward-46-%s/";
    private final String codeBridgeURL = "http://code4sa.org/";

    private final String GOOGLE_SEARCH = "Google Search";
    private final String LINKEDIN_SEARCH = "LinkedIn Search";
    private final String FACEBOOK_SEARCH = "Facebook Search";
    private final String TWITTER_SEARCH = "Twitter Search";

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

        resultsContainer = findViewById(R.id.results_container);
        demographicsContainer = findViewById(R.id.demographics_container);

        results2014 = (WebView) findViewById(R.id.results_2014);
        results2011 = (WebView) findViewById(R.id.results_2011);

        ageDemographics = (WebView) findViewById(R.id.age_demographics);
        raceDemographics = (WebView) findViewById(R.id.race_demographics);

        WebSettings webViewSettings = results2014.getSettings();
//        webViewSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webViewSettings.setJavaScriptEnabled(true);

        webViewSettings = results2011.getSettings();
//        webViewSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webViewSettings.setJavaScriptEnabled(true);

        webViewSettings = ageDemographics.getSettings();
//        webViewSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webViewSettings.setJavaScriptEnabled(true);

        webViewSettings = raceDemographics.getSettings();
//        webViewSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webViewSettings.setJavaScriptEnabled(true);

        candidateList = (LinearLayout) findViewById(R.id.candidate_list);
        locationText = (TextView) findViewById(R.id.location_text);
        pickPlace = (Button) findViewById(R.id.button_pick);
        loadingContainer = (LinearLayout) findViewById(R.id.loading_container);

        findViewById(R.id.eregardless).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(eregardlessURL));
                startActivity(Intent.createChooser(browserIntent, "Visit e-regardless"));
            }
        });

        waziMapIcon = findViewById(R.id.wazimaps);
        waziMapIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(wazimapURL));
                startActivity(Intent.createChooser(browserIntent, "Visit Wazimap"));
            }
        });

        findViewById(R.id.c4sa).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(code4saURL));
                startActivity(Intent.createChooser(browserIntent, "Visit Code4SA"));
            }
        });

        findViewById(R.id.code_bridge).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(codeBridgeURL));
                startActivity(Intent.createChooser(browserIntent, "Visit Codebridge"));
            }
        });

        findViewById(R.id.current_location).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(WardLocationActivity.this, "Getting results for your current location" , Toast.LENGTH_LONG).show();
                loadingContainer.setVisibility(View.VISIBLE);

                PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi
                    .getCurrentPlace(mGoogleApiClient, null);

                result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
                    @Override
                    public void onResult(PlaceLikelihoodBuffer likelyPlaces) {
                        if(likelyPlaces.getCount() > 0) {
//                            likelyPlaceBounds = likelyPlaces.get(0).getPlace().getViewport();
//                            likelyPlaceAddress = likelyPlaces.get(0).getPlace().getName().toString();
                            Toast.makeText(WardLocationActivity.this, "Appromixate location at: " + likelyPlaces.get(0).getPlace().getAddress().toString(), Toast.LENGTH_LONG).show();
                            locationText.setText(likelyPlaces.get(0).getPlace().getAddress().toString());
                            locationText.setVisibility(View.VISIBLE);
                            locationText.setTextColor(Color.BLACK);

                            getCandidates(likelyPlaces.get(0).getPlace().getLatLng());
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
            }
        });
        findViewById(R.id.appinvite).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String deepLinkUrl = "https://play.google.com/store/apps/details?id=com.eregardless.dololo";
                    String imageUrl = "";

                    Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
                            .setMessage(getString(R.string.invitation_message))
//                            .setDeepLink(Uri.parse(getString(R.string.invitation_deep_link)))
//                            .setCustomImage(Uri.parse(getString(R.string.invitation_custom_image)))
//                            .setCallToActionText(getString(R.string.invitation_cta))
                            .build();
                    startActivityForResult(intent, INVITE_REQUEST_CODE);
                } catch (ActivityNotFoundException ac) {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.invitation_title));
                    sendIntent.setType("text/plain");
                    startActivity(sendIntent);
                }
            }
        });

        init();
    }

    public void init() {
        if (!((LocationManager) getSystemService(Context.LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGPSPrompt();
        }
//
        loadingContainer.setVisibility(View.GONE);

        pickPlace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Construct an intent for the place picker
                try {
                    PlaceAutocomplete.IntentBuilder builder = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN);

                    builder.setFilter(new AutocompleteFilter.Builder()
                            .setTypeFilter(AutocompleteFilter.TYPE_FILTER_ADDRESS)
                            .build());

//                    if(likelyPlaceBounds != null ){
//                        builder.setBoundsBias(likelyPlaceBounds);
//                    }
//                    if(likelyPlaceAddress != null ){
//                        if(likelyPlaceAddress.length() > MAX_AUTO_COMPLETE_LENGTH)
//                            likelyPlaceAddress = likelyPlaceAddress.substring(0, MAX_AUTO_COMPLETE_LENGTH -1);
//                        builder.zzkv(likelyPlaceAddress);
//                        likelyPlaceAddress = null;
//                    }
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
        if (requestCode == INVITE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
                Log.d("Kgothatso", "Finished working..." + ids);
            } else {

                Log.e("Kgothatso", "invite send failed or cancelled:" + requestCode + ",resultCode:" + resultCode );
            }
        } else if (requestCode == REQUEST_PLACE_PICKER) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);

                locationText.setText(place.getAddress());
                locationText.setVisibility(View.VISIBLE);
                locationText.setTextColor(Color.BLACK);

                try {
                    getCandidates(place.getLatLng());

                } catch (Exception e) {
                    Toast.makeText(this, "Cant process location...", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                // TODO: Handle the error.
                Log.e("Kgothatso", status.getStatusMessage());
                locationText.setText(status.getStatusMessage());
                locationText.setVisibility(View.VISIBLE);
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


    private void getCandidates(final LatLng latLng) {
        Log.d(TAG, "Accessing: " + String.format(candidateURL, latLng.latitude, latLng.longitude ));
        actionPerformded = true;

        candidateList.removeAllViews();

        if(loadingContainer.getVisibility() != View.VISIBLE)
            loadingContainer.setVisibility(View.VISIBLE);

        Request request = new Request.Builder()
                .url(String.format(candidateURL, latLng.latitude, latLng.longitude ))
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

                        TextView error = (TextView) getLayoutInflater().inflate(android.R.layout.simple_list_item_1, candidateList, false);
                        error.setText("Internet connection is flawed... check data or proxy settings...");

                        candidateList.addView(error);
                    }
                });

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String contents = response.body().string();
                Log.d(TAG, "Retrieved: " + contents);
                try {
                    final JSONObject jsonResult = new JSONObject(contents);
                    final String ward = jsonResult.getString("ward");

                    final JSONArray candidates = jsonResult.getJSONArray("proportional representation");
                    final JSONArray councillor = jsonResult.getJSONArray("councillor");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            TextView councilorName = (TextView) findViewById(R.id.councillor_name);
                            TextView councilorMunicipality = (TextView) findViewById(R.id.councillor_municipality);
                            TextView councilorParty = (TextView) findViewById(R.id.councillor_party);

                            if(councillor.length() > 0){
                                try {
                                    councilorMunicipality.setText(councillor.getJSONObject(0).getString("Seat Type").trim() + " - " + councillor.getJSONObject(0).getString("Municipality").trim());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                try {
                                    councilorName.setText(councillor.getJSONObject(0).getString("Fullname").trim() + " " + councillor.getJSONObject(0).getString("Surname").trim());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                try {
                                    councilorParty.setText(councillor.getJSONObject(0).getString("Party").trim() );
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                councilorName.setText("Not yet know");
                                councilorParty.setText("Proportional Representation");
                                councilorMunicipality.setText("will decide your municipality");

                                for(int i = 0; i < candidates.length(); i++){

                                    try {
                                        JSONObject candidate = candidates.getJSONObject(i);
                                        Log.d(TAG, "Result: " + candidate.toString());

                                        if(i != 0)
                                            candidateList.addView(getLayoutInflater().inflate(R.layout.seperator, candidateList, false));

                                        View root = getLayoutInflater().inflate(R.layout.councillor_row, candidateList, false);
                                        final TextView resultName = (TextView) root.findViewById(R.id.candidate_name);
                                        final TextView resultParty = (TextView) root.findViewById(R.id.candidate_party);
                                        final TextView resultPROrder = (TextView) root.findViewById(R.id.candidate_pr_no);

                                        String candidateName = candidate.getString("Fullname").trim() + " " + candidate.getString("Surname").trim() ;
                                        String candidateParty = candidate.getString("Party").trim();
                                        String candidatePR = candidate.getString("Seat Type").trim() + " - " +  candidate.getString("Ward").trim();

                                        resultName.setText(candidateName);
                                        resultParty.setText(candidateParty);
                                        resultPROrder.setText(candidatePR);
                                        try {

                                            final String googleUrl = "https://www.google.com/search?q=" + URLEncoder.encode(candidateName + " - " + candidateParty, "utf-8");
                                            final String linkedInUrl = "https://www.linkedin.com/vsearch/p?type=people&keywords=" + URLEncoder.encode(resultName.getText().toString(), "utf-8");
                                            final String facebookUrl = "https://www.facebook.com/search/top/?q=" + URLEncoder.encode(candidateName, "utf-8");
                                            final String twitterUrl = "https://twitter.com/search?q=" + URLEncoder.encode(candidateName, "utf-8");

                                            View.OnClickListener openURLs = new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    final CharSequence[] options = {GOOGLE_SEARCH, LINKEDIN_SEARCH, FACEBOOK_SEARCH, TWITTER_SEARCH};
                                                    AlertDialog.Builder builder = new AlertDialog.Builder(WardLocationActivity.this);
                                                    builder.setItems(options, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int item) {
                                                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(googleUrl));
                                                            switch (options[item].toString()){
                                                                case GOOGLE_SEARCH:
                                                                    browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(googleUrl));
                                                                    break;
                                                                case LINKEDIN_SEARCH:
                                                                    browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(linkedInUrl));
                                                                    break;
                                                                case FACEBOOK_SEARCH:
                                                                    browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(facebookUrl));
                                                                    break;
                                                                case TWITTER_SEARCH:
                                                                    browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(twitterUrl));
                                                                    break;
                                                            }
                                                            startActivity(Intent.createChooser(browserIntent, "Search for " + resultName.getText().toString()));
                                                        }
                                                    });
                                                    builder.show();


                                                }
                                            };
                                            root.setOnClickListener(openURLs);

                                        } catch (UnsupportedEncodingException e) {
                                            e.printStackTrace();
                                        }

                                        candidateList.addView(root);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }


                                }
                            }

                            TextView text = (TextView) getLayoutInflater().inflate(android.R.layout.simple_list_item_1, candidateList, false);
                            text.setText("Ward No " + ward + " proportional representation");
                            text.setGravity(Gravity.CENTER);

                            candidateList.addView(text);

                            results2011.loadData(String.format(waziMap2011, ward, ward), "text/html",
                                    "utf-8");
                            results2014.loadData(String.format(waziMap2014, ward, ward), "text/html",
                                    "utf-8");

                            ageDemographics.loadData(String.format(waziMapAGE, ward, ward), "text/html",
                                    "utf-8");
                            raceDemographics.loadData(String.format(waziMapRace, ward, ward), "text/html",
                                    "utf-8");

                            waziMapIcon.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(wazimapWardURL));
                                    startActivity(Intent.createChooser(browserIntent, "Visit Wazimap"));
                                }
                            });
                            resultsContainer.setVisibility(View.VISIBLE);
                            demographicsContainer.setVisibility(View.VISIBLE);
                        }
                    });






                    Log.d(TAG, "proportional representation: " + candidates.length());
                    if(candidates.length() == 0){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView text = (TextView) getLayoutInflater().inflate(android.R.layout.simple_list_item_1, candidateList, false);
                                text.setText("Unfortanetly your ward isn't currently in the database :(");
                                candidateList.addView(text);
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
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });

    }
}
