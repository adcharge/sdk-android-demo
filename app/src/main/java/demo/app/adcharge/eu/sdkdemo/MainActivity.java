package demo.app.adcharge.eu.sdkdemo;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import eu.adcharge.api.ApiException;
import eu.adcharge.api.ApiValidationException;
import eu.adcharge.api.NoAdvertisementFoundException;
import eu.adcharge.sdk.logic.AdCharge;

public class MainActivity extends AppCompatActivity {
    String androidId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
        Log.d("androidId", androidId);
        setContentView(R.layout.activity_main);
        final Switch activate = findViewById(R.id.activate);
        final Switch showAdsOnCall = findViewById(R.id.show_ads_on_call);
        final Button showInAppAd = findViewById(R.id.show_in_app_ad);
        final EditText login = findViewById(R.id.editText);
        AdChargeDependentCode.initAdCharge(this);
        showAdsOnCall.setChecked(true); //.showAdsOnCall(true) in initial settings
        final AdCharge adCharge = ((Application) getApplication()).getAdCharge();
        activate.setChecked(adCharge.isLoggedIn());
        if (adCharge.isLoggedIn()) {
            new AsyncTask<Object, Object, Object>() {
                @Override
                protected Object doInBackground(Object[] objects) {
                    try {
                        login.setText(adCharge.getApiWrapper().getUserInfo().getUsername());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.execute();
        } else {
            login.setText(androidId);
        }
        activate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    final String username = login.getText().toString();
                    List<String> credentials = AdChargeDependentCode.getAdChargeCredentialsForUniqueUser(username);
                    new AdChargeDependentCode.LoginUserTask(credentials.get(0), credentials.get(1), MainActivity.this, adCharge).execute();
                    showInAppAd.setEnabled(true);
                } else {
                    AdChargeDependentCode.deactivateAdCharge(adCharge);
                    showInAppAd.setEnabled(false);
                }

            }
        });
        showAdsOnCall.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                adCharge.updateSettings(AdChargeDependentCode.ADCHARGE_SETTINGS.showAdsOnCall(isChecked));
            }
        });
        showInAppAd.setEnabled(adCharge.isLoggedIn());
        showInAppAd.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AdChargeDependentCode.AsyncTaskWithTimeout<Void, Void, Void>(MainActivity.this, 5, TimeUnit.SECONDS) {
                            boolean adPreloaded = false;
                            // this task might be divided in 2 different parts
                            // preload ad (if it possible to predict moment when ad supposed to be shown - preload it few seconds before usage, but no more then 30 seconds)
                            // use it
                            //
                            // preload is not required but improves performance and gives warranty there's an advertisement for user
                            // means app will not show empty activity if no ads found
                            @Override
                            protected Void runInBackground(Void... voids) {
                                try {
                                    adCharge.preloadInAppAdvertisement();
                                    adPreloaded = true;
                                } catch (NoAdvertisementFoundException e) {
                                    // just no advertisement for this user available
                                    // (normal flow, happens for different reasons)
                                } catch (ApiException | ApiValidationException | IOException e) {
                                    e.printStackTrace();
                                    // might be a bug or temporal network issue
                                    // make sense to log and collect data, such cases, if shared might help us
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                if (adPreloaded) {
                                    Intent adActivity = new Intent(getApplicationContext(), AdActivity.class);
                                    adActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(adActivity);
                                }
                            }
                        }.execute();
                    }
                }
        );
    }
}
