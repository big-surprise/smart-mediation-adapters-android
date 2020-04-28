package com.mopub.smartadserver.android.library.mediation.huawei;

import android.content.Context;

import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.HwAds;

/**
 * Mediation adapter base class that will handle initialization for all Huawei Mobile ads adapters
 */
class SASHuaweiMobileAdsAdapterBase {

    // static flag for Huawei mobile ads SDK initialization
    private static boolean huaweiMobileAdsInited = false;

    /**
     * Init method for Huawei Mobile Ads
     */
    protected void initHuaweiMobileAds(Context context, String serverParametersString) {
        String appID = getAppID(serverParametersString);
        if (!huaweiMobileAdsInited) {
            HwAds.init(context, appID);
            huaweiMobileAdsInited = true;
        }
    }

    /**
     * Utility method to get AppID from serverParametersString
     */
    protected String getAppID(String serverParametersString) {
        return serverParametersString.split("\\|")[0];
    }

    /**
     * Utility method to get AppUnitID from serverParametersString
     */
    protected String getAdUnitID(String serverParametersString) {
        String[] parameters = serverParametersString.split("\\|");
        if (parameters.length > 1) {
            return parameters[1];
        }
        return "";
    }

    /**
     * Common Huawei ads request configuration for all formats
     */
    protected AdParam configureAdRequest() {
        // create Google ad request builder
        AdParam.Builder adParamBuilder = new AdParam.Builder();

        return adParamBuilder.build();
    }
}
