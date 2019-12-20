/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.customization.model.theme.custom;

import static com.android.customization.model.ResourceConstants.STYLE_BACKGROUND_COLOR_LIGHT_NAME;
import static com.android.customization.model.ResourceConstants.STYLE_BACKGROUND_COLOR_DARK_NAME;
import static com.android.customization.model.ResourceConstants.ANDROID_PACKAGE;
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_ANDROID_THEME;
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_UISTYLE_ANDROID;
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_UISTYLE_SETTINGS;
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_UISTYLE_SYSUI;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.util.Log;

import com.android.customization.model.ResourceConstants;
import com.android.customization.model.theme.OverlayManagerCompat;
import com.android.customization.model.theme.custom.ThemeComponentOption.UiStyleOption;
import com.android.wallpaper.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link ThemeComponentOptionProvider} that reads {@link UiStyleOption}s from
 * UI Styles overlays.
 */
public class UiStyleOptionsProvider extends ThemeComponentOptionProvider<UiStyleOption> {

    private static final String TAG = "UiStyleOptionsProvider";
    private final CustomThemeManager mCustomThemeManager;
    private final String mDefaultThemePackage;

    private final List<String> mSysUiStylesOverlayPackages = new ArrayList<>();
    private final List<String> mSettingsStylesOverlayPackages = new ArrayList<>();

    public UiStyleOptionsProvider(Context context, OverlayManagerCompat manager,
            CustomThemeManager customThemeManager) {
        super(context, manager, OVERLAY_CATEGORY_UISTYLE_ANDROID);
        mCustomThemeManager = customThemeManager;
        String[] targetPackages = ResourceConstants.getPackagesToOverlay(context);
        List<String> themePackages = manager.getOverlayPackagesForCategory(
                OVERLAY_CATEGORY_ANDROID_THEME, UserHandle.myUserId(), ANDROID_PACKAGE);
        mSysUiStylesOverlayPackages.addAll(manager.getOverlayPackagesForCategory(
                OVERLAY_CATEGORY_UISTYLE_SYSUI, UserHandle.myUserId(), targetPackages));
        mSettingsStylesOverlayPackages.addAll(manager.getOverlayPackagesForCategory(
                OVERLAY_CATEGORY_UISTYLE_SETTINGS, UserHandle.myUserId(), targetPackages));
        mDefaultThemePackage = themePackages.isEmpty() ? null : themePackages.get(0);
    }

    @Override
    protected void loadOptions() {
        int accentColor = mCustomThemeManager.resolveAccentColor(mContext.getResources());
        Map<String, UiStyleOption> optionsByPrefix = new HashMap<>();
        Configuration configuration = mContext.getResources().getConfiguration();
        boolean nightMode = (configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES ? true : false;
        addDefault();

        for (String overlayPackage : mOverlayPackages) {
            UiStyleOption option = addOrUpdateOption(optionsByPrefix, overlayPackage,
                    OVERLAY_CATEGORY_UISTYLE_ANDROID);
            try {
                Resources overlayRes = getOverlayResources(overlayPackage);
                int lightColor = overlayRes.getColor(
                        overlayRes.getIdentifier(STYLE_BACKGROUND_COLOR_LIGHT_NAME, "color", overlayPackage),
                        null);
                int darkColor = overlayRes.getColor(
                        overlayRes.getIdentifier(STYLE_BACKGROUND_COLOR_DARK_NAME, "color", overlayPackage),
                        null);
                PackageManager pm = mContext.getPackageManager();
                String label = pm.getApplicationInfo(overlayPackage, 0).loadLabel(pm).toString();
                option.addStyleInfo(overlayPackage, label, lightColor, darkColor, accentColor);
                mOptions.add(option);
            } catch (NameNotFoundException | NotFoundException e) {
                Log.w(TAG, String.format("Couldn't load UI style overlay %s, will skip it",
                        overlayPackage), e);
            }
        }

        for (String overlayPackage : mSysUiStylesOverlayPackages) {
            addOrUpdateOption(optionsByPrefix, overlayPackage, OVERLAY_CATEGORY_UISTYLE_SYSUI);
        }

        for (String overlayPackage : mSettingsStylesOverlayPackages) {
            addOrUpdateOption(optionsByPrefix, overlayPackage, OVERLAY_CATEGORY_UISTYLE_SETTINGS);
        }

        /**for (UiStyleOption option : optionsByPrefix.values()) {
            if (option.isValid(mContext)) {
                option.setLabel(mContext.getString(R.string.primary_component_title, mOptions.size()));
            }
        }**/
    }

    private UiStyleOption addOrUpdateOption(Map<String, UiStyleOption> optionsByPrefix,
            String overlayPackage, String category) {
        String prefix = overlayPackage.substring(0, overlayPackage.lastIndexOf("."));
        UiStyleOption option;
        if (!optionsByPrefix.containsKey(prefix)) {
            option = new UiStyleOption();
            optionsByPrefix.put(prefix, option);
        } else {
            option = optionsByPrefix.get(prefix);
        }
        option.addOverlayPackage(category, overlayPackage);
        return option;
    }

    private void addDefault() {
        int lightColor, darkColor;
        UiStyleOption option = new UiStyleOption();
        Resources system = Resources.getSystem();
        int accentColor = mCustomThemeManager.resolveAccentColor(mContext.getResources());
        try {
            Resources r = getOverlayResources(mDefaultThemePackage);
            lightColor = r.getColor(
                    r.getIdentifier(STYLE_BACKGROUND_COLOR_LIGHT_NAME, "color", mDefaultThemePackage),
                    null);
            darkColor = r.getColor(
                    r.getIdentifier(STYLE_BACKGROUND_COLOR_DARK_NAME, "color", mDefaultThemePackage),
                    null);
        } catch (NotFoundException | NameNotFoundException e) {
            Log.d(TAG, "Didn't find default style, will use system option", e);

            lightColor = system.getColor(
                    system.getIdentifier(STYLE_BACKGROUND_COLOR_LIGHT_NAME, "color", ANDROID_PACKAGE), null);

            darkColor = system.getColor(
                    system.getIdentifier(STYLE_BACKGROUND_COLOR_DARK_NAME, "color", ANDROID_PACKAGE), null);
        }
        option.addStyleInfo(null,
                mContext.getString(R.string.default_theme_title), lightColor, darkColor, accentColor);
        option.addOverlayPackage(OVERLAY_CATEGORY_UISTYLE_SYSUI, null);
        option.addOverlayPackage(OVERLAY_CATEGORY_UISTYLE_SETTINGS, null);
        mOptions.add(option);
    }
}
