package demo.app.adcharge.eu.sdkdemo;

import eu.adcharge.sdk.logic.AdCharge;

public class Application extends android.app.Application {

    private AdCharge adCharge;

    public AdCharge getAdCharge() {
        return adCharge;
    }

    public void setAdCharge(AdCharge adCharge) {
        this.adCharge = adCharge;
    }
}