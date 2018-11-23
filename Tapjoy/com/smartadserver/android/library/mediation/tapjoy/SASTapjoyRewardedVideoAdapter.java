package com.smartadserver.android.library.mediation.tapjoy;

import android.content.Context;
import android.support.annotation.NonNull;

import com.smartadserver.android.library.mediation.SASMediationRewardedVideoAdapter;
import com.smartadserver.android.library.mediation.SASMediationRewardedVideoAdapterListener;
import com.smartadserver.android.library.util.SASUtil;
import com.tapjoy.TJActionRequest;
import com.tapjoy.TJConnectListener;
import com.tapjoy.TJError;
import com.tapjoy.TJPlacement;
import com.tapjoy.TJPlacementListener;
import com.tapjoy.TJPlacementManager;
import com.tapjoy.TJPlacementVideoListener;
import com.tapjoy.Tapjoy;

import java.util.Map;

/**
 * Mediation adapter class for Tapjoy rewarded video ad format
 */
public class SASTapjoyRewardedVideoAdapter implements SASMediationRewardedVideoAdapter {

    static private final String TAG = SASTapjoyRewardedVideoAdapter.class.getSimpleName();

    private TJPlacement tjPlacement;

    private boolean needReward = false;

    /**
     * @param context The {@link Context} needed by the mediation SDK to make the ad request.
     * @param serverParametersString a String containing all needed parameters (as returned by Smart ad delivery) to make the mediation ad call.
     * @param clientParameters additional client-side parameters (user specific, like location).
     * @param rewardedVideoAdapterListener the {@link SASMediationRewardedVideoAdapterListener} provided to this {@link com.smartadserver.android.library.mediation.SASMediationAdapter} to notify the Smart SDK of events
     */
    @Override
    public void requestRewardedVideoAd(@NonNull final Context context, @NonNull String serverParametersString, @NonNull Map<String, String> clientParameters, @NonNull final SASMediationRewardedVideoAdapterListener rewardedVideoAdapterListener) {
        SASUtil.logDebug(TAG, "SASTapjoyInterstitialAdapter adRequest");

        // Retrieve placement info -- Here serverParametersString is "SDKKey/placementName"
        String[] placementInfo = serverParametersString.split("/");

        // Check that the placement info are correctly set
        if (placementInfo.length != 2 || placementInfo[0].length() == 0 || placementInfo[1].length() == 0) {
            rewardedVideoAdapterListener.adRequestFailed("The Tapjoy SDKKey and/or placementName is not correctly set", false);
        }

        // Pass GDPR consent if applicable
        String value = clientParameters.get(GDPR_APPLIES_KEY);
        if (value != null) {
            // Smart determined GDPR applies or not
            Tapjoy.subjectToGDPR(!("false".equalsIgnoreCase(value)));
        } else {
            // leave Tapjoy make its choice on whether GDPR applies or not
        }

        // now find if we have the user consent for ad purpose, and pass it to TapJoy
        String smartConsent = clientParameters.get(GDPR_CONSENT_KEY);
        if (smartConsent != null) {
            Tapjoy.setUserConsent(smartConsent);
        }

        String SDKKey = placementInfo[0];
        final String placementName = placementInfo[1];

        // Instantiate the Tapjoy Placement Listener
        final TJPlacementListener placementListener = new TJPlacementListener() {
            @Override
            public void onRequestSuccess(TJPlacement tjPlacement) {
                SASUtil.logDebug(TAG, "placementListener onRequestSuccess");
                // check if the content is available. If not, we have a no ad.
                if (!tjPlacement.isContentAvailable()) {
                    rewardedVideoAdapterListener.adRequestFailed("Request succeed but content is not available (noad)", true);
                }
            }

            @Override
            public void onRequestFailure(TJPlacement tjPlacement, TJError tjError) {
                SASUtil.logDebug(TAG, "placementListener onRequestFailure");
                rewardedVideoAdapterListener.adRequestFailed(tjError.message, false);
            }

            @Override
            public void onContentReady(TJPlacement tjPlacement) {
                SASUtil.logDebug(TAG, "placementListener onContentReady");
                rewardedVideoAdapterListener.onRewardedVideoLoaded();
            }

            @Override
            public void onContentShow(TJPlacement tjPlacement) {
                SASUtil.logDebug(TAG, "placementListener onContentShow");
                // call the listener only when the video start to avoid counting pixel if the video have an error and does not start
            }

            @Override
            public void onContentDismiss(TJPlacement tjPlacement) {
                SASUtil.logDebug(TAG, "placementListener onContentDismiss");
                rewardedVideoAdapterListener.onAdClosed();

                if (needReward) {
                    rewardedVideoAdapterListener.onReward(null);
                }
            }

            @Override
            public void onPurchaseRequest(TJPlacement tjPlacement, TJActionRequest tjActionRequest, String s) {
                SASUtil.logDebug(TAG, "placementListener onPurchaseRequest");
            }

            @Override
            public void onRewardRequest(TJPlacement tjPlacement, TJActionRequest tjActionRequest, String s, int i) {
                SASUtil.logDebug(TAG, "placementListener onRewardRequest");
            }
        };

        // Instantiate Tapjoy Placement Video Listener
        final TJPlacementVideoListener placementVideoListener = new TJPlacementVideoListener() {
            @Override
            public void onVideoStart(TJPlacement tjPlacement) {
                SASUtil.logDebug(TAG, "placementVideoListener onVideoStart");
                rewardedVideoAdapterListener.onRewardedVideoShown();
            }

            @Override
            public void onVideoError(TJPlacement tjPlacement, String s) {
                SASUtil.logDebug(TAG, "placementVideoListener onVideoError");
                rewardedVideoAdapterListener.onRewardedVideoFailedToShow(s);
            }

            @Override
            public void onVideoComplete(TJPlacement tjPlacement) {
                SASUtil.logDebug(TAG, "placementVideoListener onVideoComplete");

                // Store that the user needs a reward
                needReward = true;
            }
        };

        // Configure the Tapjoy SDK for this call
        Tapjoy.connect(context, SDKKey, null, new TJConnectListener() {
            @Override
            public void onConnectSuccess() {
                SASUtil.logDebug(TAG, "Tapjoy onConnectSuccess");
                tjPlacement = TJPlacementManager.createPlacement(context, placementName, false, placementListener);
                tjPlacement.setVideoListener(placementVideoListener);

                tjPlacement.requestContent();
            }

            @Override
            public void onConnectFailure() {
                SASUtil.logDebug(TAG, "Tapjoy onConnectFailure");
                rewardedVideoAdapterListener.adRequestFailed("The Tapjor SDK failed to connect", false);
            }
        });
    }

    @Override
    public void showRewardedVideoAd() throws Exception {
        if (tjPlacement != null && tjPlacement.isContentAvailable()) {
            tjPlacement.showContent();
        }
    }

    @Override
    public void onDestroy() {
        tjPlacement = null;
    }
}
