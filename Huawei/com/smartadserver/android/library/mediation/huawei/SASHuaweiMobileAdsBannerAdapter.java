package com.mopub.smartadserver.android.library.mediation.huawei;

import java.util.Map;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;

import com.huawei.hms.ads.AdListener;
import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.BannerAdSize;
import com.huawei.hms.ads.banner.BannerView;
import com.smartadserver.android.library.mediation.SASMediationBannerAdapter;
import com.smartadserver.android.library.mediation.SASMediationBannerAdapterListener;

/**
 * Mediation adapter class for Huawei mobile ads banner format
 */
public class SASHuaweiMobileAdsBannerAdapter extends SASHuaweiMobileAdsAdapterBase implements SASMediationBannerAdapter {

    // tag for logging purposes
    private static final String TAG = SASHuaweiMobileAdsBannerAdapter.class.getSimpleName();

    // Huawei mobile ads banner view instance
    View adView;


    /**
     * Requests a mediated banner ad asynchronously
     *
     * @param context                the {@link Context} needed by the mediation SDK to make the ad request
     * @param serverParametersString a String containing all needed parameters (as returned by Smart ad delivery)
     *                               to make the mediation ad call
     * @param clientParameters       additional client-side parameters (user specific, like location)
     * @param bannerAdapterListener  the {@link SASMediationBannerAdapterListener} provided to
     *                               this {@link com.smartadserver.android.library.mediation.SASMediationAdapter} to notify Smart SDK of events occurring
     */
    @Override
    public void requestBannerAd(@NonNull Context context, @NonNull String serverParametersString, @NonNull Map<String, String> clientParameters,
                                @NonNull final SASMediationBannerAdapterListener bannerAdapterListener) {

        initHuaweiMobileAds(context, serverParametersString);

        String adUnitID = getAdUnitID(serverParametersString);
        BannerAdSize adSize = getAdSize(serverParametersString);

        // create huawei ad request
        AdParam adRequest = configureAdRequest();

        // Create Huawei AdView and configure it.
        BannerView bannerView = new BannerView(context);
        bannerView.setAdId(adUnitID);
        bannerView.setBannerAdSize(adSize);

        AdListener adListener = createAdListener(bannerAdapterListener, bannerView);

        // set listener on banner
        bannerView.setAdListener(adListener);

        // perform ad request
        bannerView.loadAd(adRequest);

        adView = bannerView;
    }

    private AdListener createAdListener(final SASMediationBannerAdapterListener bannerAdapterListener, final View adView) {
        // create Huawei banner listener that will intercept ad mob banner events and call appropriate
        // SASMediationBannerAdapterListener counterpart methods
        return new AdListener() {

            public void onAdClosed() {
                Log.d(TAG, "Huawei mobile ads onAdClosed for banner");
                bannerAdapterListener.onAdClosed();
            }

            public void onAdFailedToLoad(int errorCode) {
                Log.d(TAG, "Huawei mobile ads onAdFailedToLoad for banner (error code:" + errorCode + ")");
                boolean isNoAd = errorCode == AdParam.ErrorCode.NO_AD;
                bannerAdapterListener.adRequestFailed("Huawei mobile ads ad loading error code " + errorCode, isNoAd);
            }

            public void onAdLeftApplication() {
                Log.d(TAG, "Huawei mobile ads onAdLeftApplication for banner");
                bannerAdapterListener.onAdClicked();
                bannerAdapterListener.onAdLeftApplication();
            }

            public void onAdOpened() {
                Log.d(TAG, "Huawei mobile ads onAdOpened for banner");
            }

            public void onAdLoaded() {
                Log.d(TAG, "Huawei mobile ads onAdLoaded for banner");
                bannerAdapterListener.onBannerLoaded(adView);
            }
        };
    }


    /**
     * Utility method to get Banner Size from serverParametersString
     */
    protected BannerAdSize getAdSize(String serverParametersString) {
        String[] parameters = serverParametersString.split("\\|");
        int bannerSizeIndex = 0;
        if (parameters.length > 2) {
            // Extracting banner size
            bannerSizeIndex = Integer.parseInt(parameters[2]);
        }
        switch (bannerSizeIndex) {
            case 1:
                return BannerAdSize.BANNER_SIZE_300_250;
            case 2:
                return BannerAdSize.BANNER_SIZE_728_90;
        }
        return BannerAdSize.BANNER_SIZE_320_50;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Huawei mobile ads onDestroy for banner");
        if (adView != null) {
            if (adView instanceof BannerView) {
                ((BannerView) adView).destroy();
            }
        }
    }
}
