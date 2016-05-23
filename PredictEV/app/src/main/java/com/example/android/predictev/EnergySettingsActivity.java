package com.example.android.predictev;

import android.app.SearchManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.example.android.predictev.models.Utility;
import com.example.android.predictev.models.UtilityArray;
import com.example.android.predictev.services.UtilityRateAPIService;

import java.util.ArrayList;
import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EnergySettingsActivity extends AppCompatActivity {
    protected static final String TAG = "EnergySettingsActivity";
    private TextView currentUtilityTextView;
    private TextView utilityRateTextView;
    private EditText gasPriceEditText;
    private double gasPrice;
    private ListView utilityOptionsListView;
    private UtilityRateAPIService mService;
    private Retrofit retrofit;
    private String userZip;
    private String utilityName;
    private double utilityRate;
    private String utilityRateString;
    private String gasPriceString;
    private ArrayList<Utility> utilities;
    private ArrayAdapter<String> mAdapter;
    private String latString;
    private String lonString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_energy_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        latString = (String) getIntent().getExtras().get(Constants.EXTRA_USER_LAT);
        Log.i(TAG, "onCreate: latString : " + latString);
        lonString = (String) getIntent().getExtras().get(Constants.EXTRA_USER_LON);
        Log.i(TAG, "onCreate: latString : " + lonString);

        if (savedInstanceState != null) {
            utilityName = savedInstanceState.getString(Constants.KEY_SHARED_PREF_UTIL_NAME);
            Log.i(TAG, "onCreate: utilityName: " + utilityName);
            utilityRateString = savedInstanceState.getString(Constants.KEY_SHARED_PREF_UTIL_RATE);
            Log.i(TAG, "onCreate: utilityRate: " + utilityRateString);

            initLayoutElements();

            currentUtilityTextView.setText(utilityName);
            utilityRateTextView.setText("$" + utilityRateString + " / kWh");

        } else {

            initLayoutElements();
            loadSavedPreferences();
        }

        // TODO: add logic to display list of utilities if user's lat/lon returns multiple utility providers
        utilities = new ArrayList<>();
        utilityOptionsListView = (ListView) findViewById(R.id.utility_options_list_view);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                findUtilities();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void loadSavedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        utilityName = sharedPreferences.getString(Constants.KEY_SHARED_PREF_UTIL_NAME, "Please set your location.");
        utilityRateString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_UTIL_RATE, "0.0000");
        gasPriceString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_GAS_PRICE,"0.00");

        currentUtilityTextView.setText(utilityName);
        utilityRateTextView.setText(utilityRateString);

        gasPriceEditText.setText(gasPriceString);
        Log.i(TAG, "loadSavedPref gasPriceString: " + gasPriceString);

    }

    private void initLayoutElements() {

        currentUtilityTextView = (TextView) findViewById(R.id.current_utility_text_view);
        utilityRateTextView = (TextView) findViewById(R.id.utility_rate_text_view);
        utilityOptionsListView = (ListView) findViewById(R.id.utility_options_list_view);
        gasPriceEditText = (EditText) findViewById(R.id.gas_price_edit_text);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    public void findUtilities() {
        retrofit = new Retrofit.Builder().baseUrl("http://developer.nrel.gov/api/utility_rates/")
                .addConverterFactory(GsonConverterFactory.create()).build();
        mService = retrofit.create(UtilityRateAPIService.class);

        Call<UtilityArray> call = null;

        call = mService.getElectricityProviders("vIp4VQcx5zLfEr7Mi61aGd2vjIDpBpIqQRRQCoWt", latString, lonString);

        if (call != null) {
            call.enqueue(new Callback<UtilityArray>() {
                @Override
                public void onResponse(Call<UtilityArray> call, Response<UtilityArray> response) {
                    Utility[] utilityArray = response.body().getOutputs().getUtilities();
                    ArrayList<Utility> localUtilities = new ArrayList<>(Arrays.asList(utilityArray));
                    utilities.addAll(localUtilities);

                    utilityName = utilities.get(0).getUtilityName();
                    currentUtilityTextView.setText(utilityName);

                    utilityRate = response.body().getOutputs().getResidentialRate();
                    utilityRateString = String.valueOf(utilityRate);
                    utilityRateTextView.setText("$" + utilityRateString + " / kWh");


//                    TODO: set adapter to display utilities in listview if there are multiple utilities in the area
//                    ArrayList<String> utilityNames = new ArrayList<String>();
//                    utilityNames.add(utilityArray[0].getUtilityName());
//                    mAdapter = new ArrayAdapter<>(EnergySettingsActivity.this,android.R.layout.simple_list_item_1, utilityNames);
//                    utilityOptionsListView.setAdapter(mAdapter);
//
//
//                    if (getActivity() != null) {
//                        sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
//                        stringSharedPrefs = sharedPreferences.getString(MainActivity.KEY_SHARED_PREF_NOTIF, "");
//                        arrayNotificationPref = stringSharedPrefs.split(",");
//                        Log.i(TAG, "onResponse: shared prefs = " + sharedPreferences);
//                        Log.i(TAG, "onResponse: title = " + fragTitle);
//                        Log.i(TAG, "onResponse: prefs as string" + stringSharedPrefs);
//
//                        if (Arrays.asList(arrayNotificationPref).contains(fragTitle)) {
//                            // if a notification pref is on, add those articles to the database here
//                            // will use them as a reference point for notifications on new articles
//                            Log.i(TAG, "onResponse: entered if statement for database entry");
//                            DatabaseHelper searchHelper = DatabaseHelper.getInstance(getActivity());
//                            // checks if the category articles are already in the database, if not, then add them.
//                            Cursor articleCursor = searchHelper.findByCategory(fragTitle.toLowerCase());
//                            if (articleCursor.getCount() == 0) {
//                                for (Article article : articles) {
//                                    int articleId = Integer.parseInt(article.getArticleId());
//                                    String articleTitle = article.getArticleTitle();
//                                    String articleCategory = article.getArticleCategory();
//                                    String articleTimeStamp = String.valueOf(article.getArticleTimeStamp());
//                                    // adds articles to database based on users preference notifications
//                                    searchHelper.insertArticles(articleId, articleTitle, articleCategory, articleTimeStamp);
//                                }
//                            }
//                        }
//
//                        int currentSize = articleAdapter.getItemCount();
//                        articleAdapter.notifyItemRangeInserted(currentSize, articlesNew.size());
//                        alphaAdapter.notifyItemRangeInserted(currentSize, articlesNew.size());
//                    }
                }

                @Override
                public void onFailure(Call<UtilityArray> call, Throwable t) {
                }
            });
        }

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString(Constants.KEY_SHARED_PREF_UTIL_NAME, utilityName);
        savedInstanceState.putString(Constants.KEY_SHARED_PREF_UTIL_RATE, utilityRateString);
    }

    private void savePreferencesString(String key, String value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    @Override
    protected void onDestroy() {
        savePreferencesString(Constants.KEY_SHARED_PREF_UTIL_NAME, utilityName);
        Log.i(TAG, "onDestroy: utitlityName: " + utilityName);
        savePreferencesString(Constants.KEY_SHARED_PREF_UTIL_RATE, utilityRateString);

        gasPriceString = gasPriceEditText.getText().toString();
        savePreferencesString(Constants.KEY_SHARED_PREF_GAS_PRICE, gasPriceString);

        super.onDestroy();
    }
}
