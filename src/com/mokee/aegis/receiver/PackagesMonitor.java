/*
 * Copyright (C) 2015-2016 The MoKee Open Source Project
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
 *
 */

package com.mokee.aegis.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.text.TextUtils;

import com.android.internal.app.IAppOpsService;

public class PackagesMonitor extends BroadcastReceiver {

    private static final String PREF_AUTORUN = "appops_65";
    private static final String PREF_WAKELOCK = "appops_40";
    private static final String PREF_PACIFIER = "pacifier";

    @Override
    public void onReceive(Context context, Intent intent) {
        String packageName = intent.getData().getSchemeSpecificPart();
        if (!TextUtils.isEmpty(packageName)) {
            try {
                IBinder iBinder = ServiceManager.getService(Context.APP_OPS_SERVICE);
                IAppOpsService mAppOps = IAppOpsService.Stub.asInterface(iBinder);
                mAppOps.removePackageInfo(UserHandle.myUserId(), packageName);
                context.getSharedPreferences(PREF_AUTORUN, Context.MODE_PRIVATE).edit().remove(packageName).apply();
                context.getSharedPreferences(PREF_WAKELOCK, Context.MODE_PRIVATE).edit().remove(packageName).apply();
                context.getSharedPreferences(PREF_PACIFIER, Context.MODE_PRIVATE).edit().remove(packageName).apply();
            } catch (RemoteException e) {
            }
        }
    }
}
