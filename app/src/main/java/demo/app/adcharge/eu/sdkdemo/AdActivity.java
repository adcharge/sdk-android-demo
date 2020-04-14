package demo.app.adcharge.eu.sdkdemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import android.support.v7.app.AppCompatActivity;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import eu.adcharge.api.ApiException;
import eu.adcharge.api.ApiValidationException;
import eu.adcharge.api.NoAdvertisementFoundException;
import eu.adcharge.sdk.logic.AdCharge;
import eu.adcharge.sdk.logic.InAppAdvertisement;

public class AdActivity extends AppCompatActivity {

    private boolean shown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final AdCharge adCharge = ((Application) getApplication()).getAdCharge();
        new AdChargeDependentCode.AsyncTaskWithTimeout<Void, Void, InAppAdvertisement>(this, 1, TimeUnit.SECONDS) {
            @Override
            protected InAppAdvertisement runInBackground(Void... objects) {
                try {
                    return adCharge.getInAppAdvertisement();
                } catch (NoAdvertisementFoundException e) {
                    // or just no ads for this user available (normal flow)
                    // or, if ad was preloaded long time ago - it's already invalidated and new one isn't available
                    // (there are different reasons for invalidation, and better not to show this ad at all)
                } catch (ApiException | ApiValidationException | IOException e) {
                    e.printStackTrace();
                    // might be a bug or temporal network issue
                    // make sense to log and collect data, such cases, if shared, might help us
                }
                return null;
            }

            @Override
            protected void onPostExecute(final InAppAdvertisement ad) {
                if (ad == null)
                    finish(); // Something went wrong (ad has expired, or wasn't preloaded and no new ads are available)
                shown = true;
                ImageView adSpace = findViewById(R.id.banner);
                ad.onDisplayed(); // Responsible for impression report. Mandatory
                adSpace.setImageBitmap(ad.getBanner());
                adSpace.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, ad.getTrackingUri());
                        browserIntent.putExtra("android.support.customtabs.extra.SESSION", getPackageName());
                        browserIntent.putExtra("android.support.customtabs.extra.EXTRA_ENABLE_INSTANT_APPS", true);
                        startActivity(browserIntent);
                    }
                });
            }

            @Override
            protected void onTimeout() {
                finish(); // In case of any issue for set timeout - operation took too long (1 second by default)
            }
        }.execute();
        setContentView(R.layout.activity_ad);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shown) finish();
    }
}
