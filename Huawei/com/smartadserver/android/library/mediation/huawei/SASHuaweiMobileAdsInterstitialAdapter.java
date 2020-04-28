package com.mopub.smartadserver.android.library.mediation.huawei;

import java.util.Map;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.huawei.hms.ads.AdListener;
import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.InterstitialAd;
import com.smartadserver.android.library.exception.SASAdDisplayException;
import com.smartadserver.android.library.mediation.SASMediationInterstitialAdapter;
import com.smartadserver.android.library.mediation.SASMediationInterstitialAdapterListener;
import com.smartadserver.android.library.util.SASUtil;

/**
 * Mediation adapter class for Huawei mobile ads interstitial format
 */
public class SASHuaweiMobileAdsInterstitialAdapter extends SASHuaweiMobileAdsAdapterBase implements SASMediationInterstitialAdapter {

    // tag for logging purposes
    private static final String TAG = SASHuaweiMobileAdsBannerAdapter.class.getSimpleName();

    // Huawei mobile ads interstitial ad
    Object interstitialAd;

    /**
     * Requests a mediated interstitial ad asynchronously
     *
     * @param context                     the {@link Context} needed by the mediation SDK to make the ad request
     * @param serverParametersString      a String containing all needed parameters (as returned by Smart ad delivery)
     *                                    to make the mediation ad call
     * @param clientParameters            additional client-side parameters (user specific, like location)
     * @param interstitialAdapterListener the {@link SASMediationInterstitialAdapterListener} provided to
     *                                    this {@link com.smartadserver.android.library.mediation.SASMediationAdapter} to notify Smart SDK of events occurring
     */
    @Override
    public void requestInterstitialAd(@NonNull Context context, @NonNull String serverParametersString, @NonNull Map<String, String> clientParameters,
                                      @NonNull final SASMediationInterstitialAdapterListener interstitialAdapterListener) {

        initHuaweiMobileAds(context, serverParametersString);

        String adUnitID = getAdUnitID(serverParametersString);

        // create Huawei mobile ad request
        AdParam adRequest = configureAdRequest();

        // create Huawei mobile ads  listener that will intercept ad mob interstitial events and call appropriate
        // SASMediationInterstitialAdapterListener counterpart methods
        AdListener interstitialAdListener = createAdListener(interstitialAdapterListener);

        // create Huawei mobile ads interstitial ad object
        InterstitialAd interstitialAd = new InterstitialAd(context);

        // set adUnitId (from serverParametersString)
        interstitialAd.setAdId(adUnitID);

        // set AdListener on the interstitial
        interstitialAd.setAdListener(interstitialAdListener);

        // perform ad request
        interstitialAd.loadAd(adRequest);

        this.interstitialAd = interstitialAd;
    }

    private AdListener createAdListener(final SASMediationInterstitialAdapterListener interstitialAdapterListener) {
        return new AdListener() {

            public void onAdClosed() {
                Log.d(TAG, "Huawei mobile ads onAdClosed for interstitial");
                interstitialAdapterListener.onAdClosed();
            }

            public void onAdFailedToLoad(int errorCode) {
                Log.d(TAG, "Huawei mobile ads onAdFailedToLoad for interstitial (error code:" + errorCode + ")");
                boolean isNoAd = errorCode == AdParam.ErrorCode.NO_AD;
                interstitialAdapterListener.adRequestFailed("Google mobile ads ad loading error code " + errorCode, isNoAd);
            }

            public void onAdLeftApplication() {
                Log.d(TAG, "Huawei mobile ads onAdLeftApplication for interstitial");
                interstitialAdapterListener.onAdClicked();
                interstitialAdapterListener.onAdLeftApplication();
            }

            public void onAdOpened() {
                Log.d(TAG, "Huawei mobile ads onAdOpened for interstitial");
                interstitialAdapterListener.onInterstitialShown();

            }

            public void onAdLoaded() {
                Log.d(TAG, "Huawei mobile ads ad onAdLoaded for interstitial");
                interstitialAdapterListener.onInterstitialLoaded();
            }
        };
    }

    private boolean isInterstitialAdLoaded() {
        if (interstitialAd != null) {
            if (interstitialAd instanceof InterstitialAd) {
                return ((InterstitialAd) interstitialAd).isLoaded();
            }
        }
        return false;
    }

    private void showInterstitialAd() {
        if (interstitialAd != null) {
            if (interstitialAd instanceof InterstitialAd) {
                ((InterstitialAd) interstitialAd).show();
            }
        }
    }

    /**
     * Shows the previously loaded interstitial if any (or throws an exception if error)
     *
     * @throws Exception
     */
    @Override
    public void showInterstitial() throws Exception {

        /*
         * Methods of the Huawei mobile ads's InterstitialAd or RewardedVideoAd like isLoaded(), show()
         * must be called on the Main Thread or they throw an exception.
         *
         * So execute them from the Main thread, but wait for the outcome, should they throw
         * an exception (which will be stored in exceptions array)
         */

        final SASAdDisplayException[] exceptions = new SASAdDisplayException[1];

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (isInterstitialAdLoaded()) {
                        // regular interstitial case
                        showInterstitialAd();
                    } else {
                        throw new Exception("No Huawei mobile ads interstitial ad loaded !");
                    }
                } catch (Exception e) {
                    // catch potential Exception and create a SASAdDisplayException containing the message
                    exceptions[0] = new SASAdDisplayException(e.getMessage());
                }

                synchronized (this) {
                    this.notify();
                }
            }
        };

        // synchronized block to wait runnable execution outcome
        synchronized (runnable) {
            SASUtil.getMainLooperHandler().post(runnable);
            runnable.wait();
        }

        // if an exception was thrown, re-throw the exception
        if (exceptions[0] != null) {
            throw exceptions[0];
        }

    }

    @Override
    public void onDestroy() {
        interstitialAd = null;
    }
}
