/*
 * Copyright (C) 2015 The CyanogenMod Open Source Project
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
 * limitations under the License
 */

package com.android.internal.util.cm;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.ConnectivityManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class QSUtils {
    private static boolean sAvailableTilesFiltered;

    private QSUtils() {}

    public static List<String> getAvailableTiles(Context context) {
        filterTiles(context);
        return QSConstants.TILES_AVAILABLE;
    }

    public static List<String> getDefaultTiles(Context context) {
        final List<String> tiles = new ArrayList<>();
        final String defaults = context.getString(
                com.android.internal.R.string.config_defaultQuickSettingsTiles);
        if (!TextUtils.isEmpty(defaults)) {
            final String[] array = TextUtils.split(defaults, Pattern.quote(","));
            for (String item : array) {
                if (TextUtils.isEmpty(item)) {
                    continue;
                }
                tiles.add(item);
            }
            filterTiles(context, tiles);
        }
        return tiles;
    }

    public static String getDefaultTilesAsString(Context context) {
        List<String> list = getDefaultTiles(context);
        return TextUtils.join(",", list);
    }

    private static void filterTiles(Context context, List<String> tiles) {
        boolean deviceSupportsMobile = deviceSupportsMobileData(context);

        // Tiles that need conditional filtering
        Iterator<String> iterator = tiles.iterator();
        while (iterator.hasNext()) {
            String tileKey = iterator.next();
            boolean removeTile = false;
            switch (tileKey) {
                case QSConstants.TILE_CELLULAR:
                case QSConstants.TILE_HOTSPOT:
                    removeTile = !deviceSupportsMobile;
                    break;
                case QSConstants.TILE_FLASHLIGHT:
                    removeTile = !deviceSupportsFlashLight(context);
                    break;
                case QSConstants.TILE_BLUETOOTH:
                    removeTile = !deviceSupportsBluetooth();
                    break;
            }
            if (removeTile) {
                iterator.remove();
                tiles.remove(tileKey);
            }
        }
    }

    private static void filterTiles(Context context) {
        if (!sAvailableTilesFiltered) {
            filterTiles(context, QSConstants.TILES_DEFAULT);
            sAvailableTilesFiltered = true;
        }
    }

    private static boolean deviceSupportsDdsSupported(Context context) {
        TelephonyManager tm = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.isMultiSimEnabled()
                && tm.getMultiSimConfiguration() == TelephonyManager.MultiSimVariants.DSDA;
    }

    public static boolean deviceSupportsMobileData(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        return cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE);
    }

    public static boolean deviceSupportsBluetooth() {
        return BluetoothAdapter.getDefaultAdapter() != null;
    }

    public static boolean deviceSupportsFlashLight(Context context) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(
                Context.CAMERA_SERVICE);
        try {
            String[] ids = cameraManager.getCameraIdList();
            for (String id : ids) {
                CameraCharacteristics c = cameraManager.getCameraCharacteristics(id);
                Boolean flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
                if (flashAvailable != null
                        && flashAvailable
                        && lensFacing != null
                        && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    return true;
                }
            }
        } catch (CameraAccessException e) {
            // Ignore
        }
        return false;
    }
}
