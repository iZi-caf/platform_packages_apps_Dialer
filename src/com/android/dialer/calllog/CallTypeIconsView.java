/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.dialer.calllog;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.CallLog.Calls;
import android.util.AttributeSet;
import android.view.View;

import com.android.contacts.common.testing.NeededForTesting;
import com.android.contacts.common.util.BitmapUtil;
import com.android.dialer.R;
import com.android.internal.telephony.CarrierAppUtils;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * View that draws one or more symbols for different types of calls (missed calls, outgoing etc).
 * The symbols are set up horizontally. As this view doesn't create subviews, it is better suited
 * for ListView-recycling that a regular LinearLayout using ImageViews.
 */
public class CallTypeIconsView extends View {
    private List<Integer> mCallTypes = Lists.newArrayListWithCapacity(3);
    private boolean mShowVideo = false;
    private Resources mResources;
    private int mWidth;
    private int mHeight;

    /* Temporarily remove below values from "framework/base" due to the code of framework/base
            can't merge to atel.lnx.1.0-dev.1.0. */
    private static final int INCOMING_IMS_TYPE = 5;
    private static final int OUTGOING_IMS_TYPE = 6;
    private static final int MISSED_IMS_TYPE = 7;

    private static boolean mIsCarrierOneEnabled = false;

    public CallTypeIconsView(Context context) {
        this(context, null);
    }

    public CallTypeIconsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mResources = new Resources(context);
        mIsCarrierOneEnabled = isCarrierOneEnabled();
    }

    public void clear() {
        mCallTypes.clear();
        mWidth = 0;
        mHeight = 0;
        invalidate();
    }

    public void add(int callType) {
        mCallTypes.add(callType);

        final Drawable drawable = getCallTypeDrawable(callType);
        mWidth += drawable.getIntrinsicWidth() + mResources.iconMargin;
        mHeight = Math.max(mHeight, drawable.getIntrinsicHeight());
        invalidate();
    }

    public void addImsOrVideoIcon(int callType, boolean showVideo) {
        mShowVideo = showVideo;
        if (showVideo) {
            mWidth += mResources.videoCall.getIntrinsicWidth();
            mHeight = Math.max(mHeight, mResources.videoCall.getIntrinsicHeight());
            invalidate();
        } else {
            final Drawable drawable = getImsDrawable(callType);
            if (drawable != null) {
                mWidth += drawable.getIntrinsicWidth();
                mHeight = Math.max(mHeight, drawable.getIntrinsicHeight());
                invalidate();
            }
        }
    }

    private Drawable getImsDrawable(int callType) {
       switch(callType) {
         case INCOMING_IMS_TYPE:
         case OUTGOING_IMS_TYPE:
         case MISSED_IMS_TYPE:
              return mResources.imsCall;
         default:
              return null;
       }
    }

    /**
     * Determines whether the video call icon will be shown.
     *
     * @param showVideo True where the video icon should be shown.
     */
    public void setShowVideo(boolean showVideo) {
        mShowVideo = showVideo;
        if (mIsCarrierOneEnabled) {
            return;
        }

        if (showVideo) {
            mWidth += mResources.videoCall.getIntrinsicWidth();
            mHeight = Math.max(mHeight, mResources.videoCall.getIntrinsicHeight());
            invalidate();
        }
    }

    /**
     * Determines if the video icon should be shown.
     *
     * @return True if the video icon should be shown.
     */
    public boolean isVideoShown() {
        return mShowVideo;
    }

    @NeededForTesting
    public int getCount() {
        return mCallTypes.size();
    }

    @NeededForTesting
    public int getCallType(int index) {
        return mCallTypes.get(index);
    }

    private Drawable getCallTypeDrawable(int callType) {
        switch (callType) {
            case Calls.INCOMING_TYPE:
            case INCOMING_IMS_TYPE:
                return mResources.incoming;
            case Calls.OUTGOING_TYPE:
            case OUTGOING_IMS_TYPE:
                return mResources.outgoing;
            case Calls.MISSED_TYPE:
            case MISSED_IMS_TYPE:
                return mResources.missed;
            case Calls.VOICEMAIL_TYPE:
                return mResources.voicemail;
            default:
                // It is possible for users to end up with calls with unknown call types in their
                // call history, possibly due to 3rd party call log implementations (e.g. to
                // distinguish between rejected and missed calls). Instead of crashing, just
                // assume that all unknown call types are missed calls.
                return mResources.missed;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int left = 0;
        for (Integer callType : mCallTypes) {
            final Drawable drawable = getCallTypeDrawable(callType);
            final int right = left + drawable.getIntrinsicWidth();
            drawable.setBounds(left, 0, right, drawable.getIntrinsicHeight());
            drawable.draw(canvas);
            left = right + mResources.iconMargin;
        }

        // If showing the video call icon, draw it scaled appropriately.
        if (mShowVideo) {
            final Drawable drawable = mResources.videoCall;
            final int right = left + drawable.getIntrinsicWidth();
            drawable.setBounds(left, 0, right, drawable.getIntrinsicHeight());
            drawable.draw(canvas);
            left = right + mResources.iconMargin;
        }

        for (Integer callType : mCallTypes) {
            final Drawable drawableIms = getImsDrawable(callType);
            if (drawableIms != null) {
                final int right = left + drawableIms.getIntrinsicWidth();
                drawableIms.setBounds(left, 0, right, drawableIms.getIntrinsicHeight());
                drawableIms.draw(canvas);
            }
        }
    }

    protected static boolean isCarrierOneEnabled() {
        CarrierAppUtils.CARRIER carrier = CarrierAppUtils.getCarrierId();
        return (carrier != null && (CarrierAppUtils.CARRIER.TELEPHONY_CARRIER_ONE
                == carrier));
    }

    private static class Resources {

        /**
         * Drawable representing an incoming answered call.
         */
        public final Drawable incoming;

        /**
         * Drawable respresenting an outgoing call.
         */
        public final Drawable outgoing;

        /**
         * Drawable representing an incoming missed call.
         */
        public final Drawable missed;

        /**
         * Drawable representing a voicemail.
         */
        public final Drawable voicemail;

        /**
         * Drawable repesenting a video call.
         */
        public final Drawable videoCall;

        /**
         * The margin to use for icons.
         */
        public final int iconMargin;

        /**
         * Drawable repesenting a wifi call.
         */
        public final Drawable wifiCall;

        /**
         * Drawable repesenting a IMS call.
         */
        public final Drawable imsCall;
        /**
         * Configures the call icon drawables.
         * A single white call arrow which points down and left is used as a basis for all of the
         * call arrow icons, applying rotation and colors as needed.
         *
         * @param context The current context.
         */
        public Resources(Context context) {
            final android.content.res.Resources r = context.getResources();

            incoming = r.getDrawable(R.drawable.ic_call_arrow);
            incoming.setColorFilter(r.getColor(R.color.answered_incoming_call), PorterDuff.Mode.MULTIPLY);

            // Create a rotated instance of the call arrow for outgoing calls.
            outgoing = BitmapUtil.getRotatedDrawable(r, R.drawable.ic_call_arrow, 180f);
            outgoing.setColorFilter(r.getColor(R.color.answered_outgoing_call), PorterDuff.Mode.MULTIPLY);

            // Need to make a copy of the arrow drawable, otherwise the same instance colored
            // above will be recolored here.
            missed = r.getDrawable(R.drawable.ic_call_arrow).mutate();
            missed.setColorFilter(r.getColor(R.color.missed_call), PorterDuff.Mode.MULTIPLY);

            voicemail = r.getDrawable(R.drawable.ic_call_voicemail_holo_dark);

            // Get the video call icon, scaled to match the height of the call arrows.
            // We want the video call icon to be the same height as the call arrows, while keeping
            // the same width aspect ratio.
            if (mIsCarrierOneEnabled) {
                videoCall = r.getDrawable(R.drawable.volte_video).mutate();
            } else {
                Bitmap videoIcon = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.ic_videocam_24dp);
                int scaledHeight = missed.getIntrinsicHeight();
                int scaledWidth = (int) ((float) videoIcon.getWidth() *
                     ((float) missed.getIntrinsicHeight() /
                             (float) videoIcon.getHeight()));
                Bitmap scaled = Bitmap.createScaledBitmap(videoIcon, scaledWidth,
                     scaledHeight, false);
                videoCall = new BitmapDrawable(context.getResources(), scaled);
            }
            videoCall.setColorFilter(r.getColor(R.color.dialtacts_secondary_text_color),
                PorterDuff.Mode.MULTIPLY);

            iconMargin = r.getDimensionPixelSize(R.dimen.call_log_icon_margin);

            wifiCall = r.getDrawable(R.drawable.vowifi_services_wifi_calling).mutate();
            wifiCall.setColorFilter(r.getColor(R.color.dialtacts_secondary_text_color),
                    PorterDuff.Mode.MULTIPLY);

            imsCall = r.getDrawable(R.drawable.volte_hd).mutate();
            imsCall.setColorFilter(r.getColor(R.color.dialtacts_secondary_text_color),
                    PorterDuff.Mode.MULTIPLY);
        }
    }
}
