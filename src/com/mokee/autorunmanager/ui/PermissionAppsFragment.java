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

package com.mokee.autorunmanager.ui;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.view.View;
import android.widget.TextView;

import com.mokee.autorunmanager.R;
import com.mokee.autorunmanager.model.PermissionApps;
import com.mokee.autorunmanager.model.PermissionApps.Callback;
import com.mokee.autorunmanager.model.PermissionApps.PermissionApp;
import com.mokee.cloud.misc.CloudUtils;

public final class PermissionAppsFragment extends PermissionsFrameFragment implements Callback, Preference.OnPreferenceChangeListener {

    private static final String PREF_CATEGORY_ALLOW_KEY = "pref_category_allow_key";
    private static final String PREF_CATEGORY_DENY_KEY = "pref_category_deny_key";

    private PermissionApps mPermissionApps;
    private AppOpsManager mAppOpsManager;

    private PreferenceScreen screenRoot;
    private PreferenceCategory categoryAllow;
    private PreferenceCategory categoryDeny;

    public static PermissionAppsFragment newInstance() {
        return new PermissionAppsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAppOpsManager = (AppOpsManager) getActivity().getSystemService(Context.APP_OPS_SERVICE);
        setLoading(true /* loading */, false /* animate */);
        mPermissionApps = new PermissionApps(getActivity(), this);
        mPermissionApps.refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        mPermissionApps.refresh();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    protected void onSetEmptyText(TextView textView) {
        textView.setText(R.string.no_apps);
    }

    @Override
    public void onPermissionsLoaded(PermissionApps permissionApps) {
        Context context = getPreferenceManager().getContext();

        if (context == null) {
            return;
        }

        screenRoot = getPreferenceScreen();
        categoryAllow = (PreferenceCategory) screenRoot.findPreference(PREF_CATEGORY_ALLOW_KEY);
        if (categoryAllow == null) {
            categoryAllow = new PreferenceCategory(context);
            categoryAllow.setKey(PREF_CATEGORY_ALLOW_KEY);
            categoryAllow.setTitle(R.string.allow_list_category);
            screenRoot.addPreference(categoryAllow);
        }

        categoryDeny = (PreferenceCategory) screenRoot.findPreference(PREF_CATEGORY_DENY_KEY);
        if (categoryDeny == null) {
            categoryDeny = new PreferenceCategory(context);
            categoryDeny.setKey(PREF_CATEGORY_DENY_KEY);
            categoryDeny.setTitle(R.string.deny_list_category);
            screenRoot.addPreference(categoryDeny);
        }
        if (!CloudUtils.Verified) return;
        for (final PermissionApp app : permissionApps.getApps()) {
            String key = app.getKey();
            SwitchPreference existingPref = (SwitchPreference) screenRoot.findPreference(key);
            if (existingPref != null) {
                existingPref.setChecked(app.getAllowed());
                continue;
            }

            final SwitchPreference pref = new SwitchPreference(context);
            pref.setKey(app.getKey());
            pref.setIcon(app.getIcon());
            pref.setTitle(app.getLabel());
            pref.setChecked(app.getAllowed());
            pref.setOnPreferenceChangeListener(this);
            if (app.getAllowed()) {
                categoryAllow.addPreference(pref);
            } else {
                categoryDeny.addPreference(pref);
            }
        }
        setLoading(false /* loading */, true /* animate */);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        PermissionApp app = mPermissionApps.getApp(preference.getKey());
        mAppOpsManager.setMode(AppOpsManager.OP_BOOT_COMPLETED, app.getUid(),
                app.getPackageName(), (Boolean) newValue ? AppOpsManager.MODE_ALLOWED : mAppOpsManager.MODE_IGNORED);
        if (!(Boolean) newValue) {
            categoryAllow.removePreference(preference);
            categoryDeny.addPreference(preference);
        } else {
            categoryDeny.removePreference(preference);
            categoryAllow.addPreference(preference);
        }
        if (categoryAllow.getPreferenceCount() == 0) {
            screenRoot.removePreference(categoryAllow);
        } else {
            if (screenRoot.findPreference(PREF_CATEGORY_ALLOW_KEY) == null) {
                screenRoot.addPreference(categoryAllow);
                if (screenRoot.findPreference(PREF_CATEGORY_DENY_KEY) != null) {
                    screenRoot.removePreference(categoryDeny);
                    screenRoot.addPreference(categoryDeny);
                }
            }
        }
        if (categoryDeny.getPreferenceCount() == 0) {
            screenRoot.removePreference(categoryDeny);
        } else {
            if (screenRoot.findPreference(PREF_CATEGORY_DENY_KEY) == null) {
                screenRoot.addPreference(categoryDeny);
            }
        }
        return true;
    }
}
