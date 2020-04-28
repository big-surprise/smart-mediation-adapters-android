package com.mopub.smartadserver.android.library.mediation.huawei;

import java.util.Map;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.reward.Reward;
import com.huawei.hms.ads.reward.RewardAd;
import com.huawei.hms.ads.reward.RewardAdListener;
import com.smartadserver.android.library.exception.SASAdDisplayException;
import com.smartadserver.android.library.mediation.SASMediationRewardedVideoAdapter;
import com.smartadserver.android.library.mediation.SASMediationRewardedVideoAdapterListener;
import com.smartadserver.android.library.model.SASReward;
import com.smartadserver.android.library.util.SASUtil;

/**
 * Mediation adapter class for Huawei rewarded video format
 */
public class SASHuaweiMobileAdsRewardedVideoAdapter extends SASHuaweiMobileAdsAdapterBase implements SASMediationRewardedVideoAdapter {

    // tag for logging purposes
    private static final String TAG = SASHuaweiMobileAdsRewardedVideoAdapter.class.getSimpleName();

    // Huawei mobile ads rewarded video manager singleton instance
    RewardAd rewardedVideoAd;

    /**
     * Requests a mediated rewarded video ad asynchronously.
     *
     * @param context                      The {@link Context} needed by the mediation SDK to make the ad request.
     * @param serverParametersString       a String containing all needed parameters (as returned by Smart ad delivery) to make the mediation ad call.
     * @param clientParameters             additional client-side parameters (user specific, like location).
     * @param rewardedVideoAdapterListener the {@link SASMediationRewardedVideoAdapterListener} provided to this {@link com.smartadserver.android.library.mediation.SASMediationAdapter} to notify the Smart SDK of events
     */
    @Override
    public void requestRewardedVideoAd(@NonNull Context context, @NonNull String serverParametersString,
                                       @NonNull Map<String, String> clientParameters, final @NonNull SASMediationRewardedVideoAdapterListener rewardedVideoAdapterListener) {

        initHuaweiMobileAds(context, serverParametersString);

        String adUnitID = getAdUnitID(serverParametersString);

        // Get Huawei mobile ads rewarded video manager instance
        rewardedVideoAd = RewardAd.createRewardAdInstance(context);

        // create an Huawei mobile ads rewarded video ad listener that will intercept ad mob interstitial events and
        // call appropriate SASMediationRewardedVideoAdapterListener counterpart methods
        RewardAdListener rewardedVideoListener = new RewardAdListener() {

            @Override
            public void onRewardAdLoaded() {
                Log.d(TAG, "Huawei mobile ads onRewardAdLoaded for rewarded video");
                rewardedVideoAdapterListener.onRewardedVideoLoaded();
            }

            @Override
            public void onRewardAdOpened() {
                Log.d(TAG, "Huawei mobile ads onRewardAdOpened for rewarded video");
                rewardedVideoAdapterListener.onRewardedVideoShown();
            }

            @Override
            public void onRewardAdStarted() {
                Log.d(TAG, "Huawei mobile ads onRewardAdStarted for interstitial");
            }

            @Override
            public void onRewardAdClosed() {
                Log.d(TAG, "Huawei mobile ads onRewardAdClosed for interstitial");
                rewardedVideoAdapterListener.onAdClosed();
            }

            @Override
            public void onRewarded(Reward rewardItem) {
                Log.d(TAG,
                        "Huawei mobile ads onRewarded for rewarded video : label:" + rewardItem.getName() + " amount:" + rewardItem.getAmount());

                // notify Smart SDK of earned reward
                rewardedVideoAdapterListener.onReward(new SASReward(rewardItem.getName(), rewardItem.getAmount()));
            }

            @Override
            public void onRewardAdLeftApp() {
                Log.d(TAG, "Huawei mobile ads onRewardAdLeftApp for interstitial");
                rewardedVideoAdapterListener.onAdClicked();
            }

            @Override
            public void onRewardAdFailedToLoad(int errorCode) {
                Log.d(TAG, "Huawei mobile ads rewarded video ad onRewardAdFailedToLoad (error code:" + errorCode + ")");
                boolean isNoAd = errorCode == AdParam.ErrorCode.NO_AD;
                rewardedVideoAdapterListener.adRequestFailed("Error code:" + errorCode, isNoAd);
            }

            @Override
            public void onRewardAdCompleted() {
                Log.d(TAG, "Huawei mobile ads onRewardAdCompleted for rewarded video");
            }

        };

        // set the rewarded video listener on the singleton
        rewardedVideoAd.setRewardAdListener(rewardedVideoListener);
        // create rewarded ad request
        AdParam adParam = configureAdRequest();
        // make ad call
        rewardedVideoAd.loadAd(adUnitID, adParam);
    }

    /**
     * Shows the previously loaded rewarded video if any (or throws an exception if error).
     *
     * @throws Exception
     */
    @Override
    public void showRewardedVideoAd() throws Exception {

        /**
         * Methods of the Huawei mobile ads's InterstitialAd or RewardAd like isLoaded(), show()
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
                    if (rewardedVideoAd.isLoaded()) {
                        rewardedVideoAd.show();
                    } else {
                        throw new Exception("No Huawei mobile ads rewarded video loaded !");
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
        // nothing to do
    }
}
