package com.droidcluster.solitaire;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

import com.droidcluster.solitaire.util.Util;

public class SettingsActivity extends PreferenceActivity {

    private static final int CARD_BACKGROUND_RESULT = 23;
    private static final int GAME_BACKGROUND_RESULT = 24;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.updateFullScreen(getWindow());

        setupSimplePreferencesScreen();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

    }

    private void setupSimplePreferencesScreen() {
        initGeneralSettings();
        initGameBgSettings(getResources().getString(R.string.settings_gamebg), R.xml.pref_game_bg,
                getString(R.string.pref_game_background_enable), getString(R.string.pref_game_background),
                GAME_BACKGROUND_RESULT);
        initGameBgSettings(getResources().getString(R.string.settings_cardbg), R.xml.pref_card_bg,
                getString(R.string.pref_card_background_enable), getString(R.string.pref_card_background),
                CARD_BACKGROUND_RESULT);
    }

    private void initGameBgSettings(String header, int resId, String keyUseDefault, final String keyImageBg,
            final int activityResultKey) {
        PreferenceCategory fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(header);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(resId);

        final Preference bgImage = findPreference(keyImageBg);
        boolean useCustom = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(keyUseDefault, false);
        bgImage.setEnabled(useCustom);
        bgImage.setSelectable(useCustom);

        findPreference(keyUseDefault).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                bgImage.setSelectable((Boolean) newValue);
                bgImage.setEnabled((Boolean) newValue);
                return true;
            }
        });
        bgImage.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(
                        Intent.createChooser(intent, getResources().getString(R.string.settings_select_picture)),
                        activityResultKey);
                return true;
            }
        });
    }

    private void initGeneralSettings() {
        addPreferencesFromResource(R.xml.pref_general);
        findPreference(getString(R.string.pref_full_screen)).setOnPreferenceChangeListener(
                new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        Util.updateFullScreen(getWindow(), (Boolean) newValue);
                        return true;
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Uri selectedImage = data.getData();
            String filePath;

            if (Build.VERSION.SDK_INT < 19) {
                String[] filePathColumn = { MediaStore.Images.Media.DATA };

                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                filePath = cursor.getString(columnIndex);
                cursor.close();
            } else {
                File tempFile = new File(getFilesDir(), "tmp_" + UUID.randomUUID().toString());

                // Copy Uri contents into temp File.
                try {
                    tempFile.createNewFile();
                    Util.copyAndClose(getContentResolver().openInputStream(selectedImage), new FileOutputStream(
                            tempFile));
                    filePath = tempFile.getAbsolutePath();
                } catch (IOException e) {
                    filePath = null;
                }

            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit()
                    .putString(
                            requestCode == CARD_BACKGROUND_RESULT ? getString(R.string.pref_card_background)
                                    : getString(R.string.pref_game_background), filePath).commit();
        }
    }
}
