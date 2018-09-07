package demo.app.adcharge.eu.sdkdemo;

import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import eu.adcharge.api.ApiException;
import eu.adcharge.api.ApiValidationException;
import eu.adcharge.api.entities.Gender;
import eu.adcharge.api.entities.User;
import eu.adcharge.sdk.logic.AdCharge;

public class MainActivity extends AppCompatActivity {
    AdCharge adCharge = null;
    String androidId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
        Log.d("androidId", androidId);
        setContentView(R.layout.activity_main);

        final Switch useAdcharge = findViewById(R.id.switch1);
        try {
            adCharge = new AdCharge(BuildConfig.SERVER_URL, getApplicationContext(), BuildConfig.SHOW_SMALL_BANNER);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        useAdcharge.setChecked(adCharge.isLoggedIn());

        final EditText login = findViewById(R.id.editText);
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
        useAdcharge.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    final String username = login.getText().toString();
                    final String password = androidId;
                    new AsyncTask<Object, Object, Object>() {
                        @Override
                        protected Object doInBackground(Object[] objects) {
                            try {
                                adCharge.login(username, password, BuildConfig.INDIVIDUAL_KEY, MainActivity.this);
                            } catch (ApiException | IOException | ApiValidationException e) {
                                User toBeRegistered = new User();
                                toBeRegistered.setUsername(username);
                                toBeRegistered.setPassword(password);
                                toBeRegistered.setGender(new Random().nextBoolean() ? Gender.MALE : Gender.FEMALE);
                                Date date = null;
                                try {
                                    date = new SimpleDateFormat("yyyy-mm-dd").parse((new Random().nextBoolean() ? "1992" : "1896") + "-01-01");
                                } catch (Exception ex) {
                                    Log.e("date", "parse error", ex);
                                }
                                toBeRegistered.setBirthday(date);
                                Log.d("toBeRegistered", toBeRegistered.toString());
                                try {
                                    adCharge.registerSubscriberUser(toBeRegistered, BuildConfig.INDIVIDUAL_KEY);
                                    // some confirmation code for user which sent with third party channel
                                    String code = "0000";
                                    adCharge.confirmUser(code);
                                    adCharge.login(username, password, BuildConfig.INDIVIDUAL_KEY, MainActivity.this);
                                } catch (ApiValidationException e1) {
                                    e1.printStackTrace();
                                } catch (ApiException e1) {
                                    e1.printStackTrace();
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                            }
                            return null;
                        }
                    }.execute();

                } else {
                    new AsyncTask<Object, Object, Object>() {
                        @Override
                        protected Object doInBackground(Object[] objects) {
                            adCharge.logout();
                            return null;
                        }
                    }.execute();

                }
            }
        });
    }
}
