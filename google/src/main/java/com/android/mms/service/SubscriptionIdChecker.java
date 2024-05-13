/*
 * Copyright 2024 Phillip Scott
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mms.service;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.provider.Telephony;
import android.util.Log;

import com.google.android.mms.util.SqliteWrapper;

public class SubscriptionIdChecker {
    private static final String TAG = "SubscriptionIdChecker";

    private static SubscriptionIdChecker sInstance;
    private boolean mCanUseSubscriptionId = false;

    // I met a device which does not have Telephony.Mms.SUBSCRIPTION_ID event if it's API Level is 22.
    private void check(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Cursor c = null;
            try {
                c = SqliteWrapper.query(context, context.getContentResolver(),
                        Telephony.Mms.CONTENT_URI,
                        new String[]{Telephony.Mms.SUBSCRIPTION_ID}, null, null, null);
                if (c != null) {
                    mCanUseSubscriptionId = true;
                }
            } catch (SQLiteException e) {
                Log.e(TAG, "SubscriptionIdChecker.check() fail");
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
    }

    public static synchronized SubscriptionIdChecker getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SubscriptionIdChecker();
            sInstance.check(context);
        }
        return sInstance;
    }

    public boolean canUseSubscriptionId() {
        return mCanUseSubscriptionId;
    }
}
