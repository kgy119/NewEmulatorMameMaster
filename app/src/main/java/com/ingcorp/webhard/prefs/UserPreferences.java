/*
 * This file is part of MAME4droid.
 *
 * Copyright (C) 2024 David Valdeita (Seleuco)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Linking MAME4droid statically or dynamically with other modules is
 * making a combined work based on MAME4droid. Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * In addition, as a special exception, the copyright holders of MAME4droid
 * give you permission to combine MAME4droid with free software programs
 * or libraries that are released under the GNU LGPL and with code included
 * in the standard release of MAME under the MAME License (or modified
 * versions of such code, with unchanged license). You may copy and
 * distribute such a system following the terms of the GNU GPL for MAME4droid
 * and the licenses of the other code concerned, provided that you include
 * the source code of that other code when and as the GNU GPL requires
 * distribution of source code.
 *
 * Note that people who make modified versions of MAME4idroid are not
 * obligated to grant this special exception for their modified versions; it
 * is their choice whether to do so. The GNU General Public License
 * gives permission to release a modified version without this exception;
 * this exception also makes it possible to release a modified version
 * which carries forward this exception.
 *
 * MAME4droid is dual-licensed: Alternatively, you can license MAME4droid
 * under a MAME license, as set out in http://mamedev.org/
 */

package com.ingcorp.webhard.prefs;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import com.ingcorp.webhard.Emulator;
import com.ingcorp.webhard.R;
import com.ingcorp.webhard.helpers.MainHelper;
import com.ingcorp.webhard.helpers.PrefsHelper;
import com.ingcorp.webhard.helpers.ScraperHelper;
import com.ingcorp.webhard.input.ControlCustomizer;
import com.ingcorp.webhard.input.GameController;

public class UserPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	private SharedPreferences settings;

	protected ListPreference mPrefGlobalVideoRenderMode;
	protected ListPreference mPrefResolution;
	protected ListPreference mPrefOSDResolution;
	protected ListPreference mPrefPortraitMode;
    protected ListPreference mPrefLandsMode;
	protected ListPreference mPrefOverlay;
	protected ListPreference mPrefOrientation;
    protected ListPreference mPrefControllerType;
    protected ListPreference mPrefAnalogDZ;
    protected ListPreference mPrefGamepadDZ;
    protected ListPreference mPrefTiltDZ;
    protected ListPreference mPrefTiltNeutral;

    protected ListPreference mPrefSound;
    protected ListPreference mPrefStickType;
    protected ListPreference mPrefNumButtons;
    protected ListPreference mPrefSizeButtons;
	protected ListPreference mPrefAlphaButtons;
    protected ListPreference mPrefSizeStick;

    protected ListPreference mPrefMainThPr;
    protected ListPreference mPrefSoundEngine;

    protected ListPreference mPrefNavbar;
    protected EditTextPreference mPrefInstPath;

	protected ListPreference mPrefShader;

	protected ListPreference mPrefNumProcessors;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// ★★★ setContentView 후 전체화면 설정 ★★★
		enableFullScreen();

		addPreferencesFromResource(R.xml.userpreferences);

		settings = PreferenceManager.getDefaultSharedPreferences(this);

		mPrefGlobalVideoRenderMode = (ListPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_GLOBAL_VIDEO_RENDER_MODE);
		mPrefResolution = (ListPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_EMU_RESOLUTION);
		mPrefOSDResolution = (ListPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_EMU_RESOLUTION_OSD);
        mPrefPortraitMode = (ListPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_PORTRAIT_SCALING_MODE);
        mPrefLandsMode = (ListPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_LANDSCAPE_SCALING_MODE);

		mPrefOverlay = (ListPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_OVERLAY);
		mPrefOrientation = (ListPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_ORIENTATION);

        mPrefControllerType = (ListPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_CONTROLLER_TYPE);
        mPrefAnalogDZ = (ListPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_ANALOG_DZ);
        mPrefGamepadDZ = (ListPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_GAMEPAD_DZ);
        mPrefTiltDZ = (ListPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_TILT_DZ);
        mPrefTiltNeutral = (ListPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_TILT_NEUTRAL);

        mPrefSound = (ListPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_EMU_SOUND);
        mPrefStickType = (ListPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_STICK_TYPE);
        mPrefNumButtons = (ListPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_NUMBUTTONS);
        mPrefSizeButtons = (ListPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_BUTTONS_SIZE);
		mPrefAlphaButtons = (ListPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_BUTTONS_ALPHA);
        mPrefSizeStick = (ListPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_STICK_SIZE);
        mPrefMainThPr = (ListPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_MAIN_THREAD_PRIORITY);
        mPrefSoundEngine = (ListPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_SOUND_ENGINE);

		//mPrefOverlayInt = (ListPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_OVERLAY_INTENSITY);

		mPrefNavbar = (ListPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_GLOBAL_NAVBAR_MODE);
		mPrefInstPath = (EditTextPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_INSTALLATION_DIR);
		mPrefShader= (ListPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_SHADER_EFFECT);
		populateShaders(mPrefShader);

		mPrefNumProcessors = (ListPreference)getPreferenceScreen().findPreference(PrefsHelper.PREF_EMU_NUM_PROCESSORS);
	}


	  @Override
	    protected void onResume() {
	        super.onResume();
	        boolean enable;
	        // Setup the initial values
	        //mCheckBoxPreference.setSummary(sharedPreferences.getBoolean(key, false) ? "Disable this setting" : "Enable this setting");
	        mPrefGlobalVideoRenderMode.setSummary("Current value is '" + mPrefGlobalVideoRenderMode.getEntry()+"'");

	        mPrefResolution.setSummary("Current value is '" + mPrefResolution.getEntry()+"'");
		    mPrefOSDResolution.setSummary("Current value is '" + mPrefOSDResolution.getEntry()+"'");
	        mPrefPortraitMode.setSummary("Current value is '" + mPrefPortraitMode.getEntry()+"'");
	        mPrefLandsMode.setSummary("Current value is '" + mPrefLandsMode.getEntry()+"'");
			mPrefOverlay.setSummary("Current value is '" + mPrefOverlay.getEntry()+"'");
		    mPrefOrientation.setSummary("Current value is '" + mPrefOrientation.getEntry()+"'");

	        mPrefControllerType.setSummary("Current value is '" + mPrefControllerType.getEntry()+"'");

	        mPrefAnalogDZ.setSummary("Current value is '" + mPrefAnalogDZ.getEntry()+"'");
	        mPrefGamepadDZ.setSummary("Current value is '" + mPrefGamepadDZ.getEntry()+"'");
	        mPrefTiltDZ.setSummary("Current value is '" + mPrefTiltDZ.getEntry()+"'");
	        mPrefTiltNeutral.setSummary("Current value is '" + mPrefTiltNeutral.getEntry()+"'");
	        mPrefSound.setSummary("Current value is '" + mPrefSound.getEntry()+"'");
	        mPrefStickType.setSummary("Current value is '" + mPrefStickType.getEntry()+"'");
	        mPrefNumButtons.setSummary("Current value is '" + mPrefNumButtons.getEntry()+"'");
	        mPrefSizeButtons.setSummary("Current value is '" + mPrefSizeButtons.getEntry()+"'");
		    mPrefAlphaButtons.setSummary("Current value is '" + mPrefAlphaButtons.getEntry()+"'");
	        mPrefSizeStick.setSummary("Current value is '" + mPrefSizeStick.getEntry()+"'");
	        mPrefMainThPr.setSummary("Current value is '" + mPrefMainThPr.getEntry()+"'");
	        mPrefSoundEngine.setSummary("Current value is '" + mPrefSoundEngine.getEntry()+"'");
	        mPrefNavbar.setSummary("Current value is '" + mPrefNavbar.getEntry()+"'");
			mPrefInstPath.setSummary("Current value is '" + mPrefInstPath.getText()+"'");
		    mPrefShader.setSummary("Current value is '" + mPrefShader.getEntry()+"'");
		    mPrefNumProcessors.setSummary("Current value is '" + mPrefNumProcessors.getEntry()+"'");

	        // Set up a listener whenever a key changes
	        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

	    }

	    @Override
	    protected void onPause() {
	        super.onPause();

	        // Unregister the listener whenever a key changes
	        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	    }

	    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
	        // Let's do something a preference values changes
	    	/*
	        if (key.equals(KEY_CHECKBOX_PREFERENCE)) {
	          mCheckBoxPreference.setSummary(sharedPreferences.getBoolean(key, false) ? "Disable this setting" : "Enable this setting");
	        }
	        else*/
	        if (key.equals(PrefsHelper.PREF_PORTRAIT_SCALING_MODE))
	        {
	            mPrefPortraitMode.setSummary("Current value is '" + mPrefPortraitMode.getEntry()+"'");
	        }
	        else if(key.equals(PrefsHelper.PREF_LANDSCAPE_SCALING_MODE))
	        {
	        	mPrefLandsMode.setSummary("Current value is '" + mPrefLandsMode.getEntry()+"'");
	        }
			else if(key.equals(PrefsHelper.PREF_OVERLAY))
			{
				mPrefOverlay.setSummary("Current value is '" + mPrefOverlay.getEntry()+"'");
			}
			else if(key.equals(PrefsHelper.PREF_ORIENTATION))
			{
				mPrefOrientation.setSummary("Current value is '" + mPrefOrientation.getEntry()+"'");
			}
	        else if(key.equals(PrefsHelper.PREF_CONTROLLER_TYPE))
	        {
	            mPrefControllerType.setSummary("Current values is '" + mPrefControllerType.getEntry()+"'");
	        }
	        else if(key.equals(PrefsHelper.PREF_GLOBAL_VIDEO_RENDER_MODE))
	        {
				mPrefGlobalVideoRenderMode.setSummary("Current value is '" + mPrefGlobalVideoRenderMode.getEntry()+"'");
				boolean enable = Integer.valueOf(mPrefGlobalVideoRenderMode.getValue()).intValue() ==PrefsHelper.PREF_RENDER_GL;
	        }
	        else if(key.equals(PrefsHelper.PREF_EMU_RESOLUTION))
	        {
	        	mPrefResolution.setSummary("Current value is '" + mPrefResolution.getEntry()+"'");
	        }
			else if(key.equals(PrefsHelper.PREF_EMU_RESOLUTION_OSD))
			{
				mPrefOSDResolution.setSummary("Current value is '" + mPrefOSDResolution.getEntry()+"'");
			}
	        else if(key.equals(PrefsHelper.PREF_ANALOG_DZ))
	        {
	        	mPrefAnalogDZ.setSummary("Current value is '" + mPrefAnalogDZ.getEntry()+"'");
	        }
	        else if(key.equals(PrefsHelper.PREF_GAMEPAD_DZ))
	        {
	        	mPrefGamepadDZ.setSummary("Current value is '" + mPrefGamepadDZ.getEntry()+"'");
	        }
	        else if(key.equals(PrefsHelper.PREF_TILT_DZ))
	        {
	        	mPrefTiltDZ.setSummary("Current value is '" + mPrefTiltDZ.getEntry()+"'");
	        }
	        else if(key.equals(PrefsHelper.PREF_TILT_NEUTRAL))
	        {
	        	mPrefTiltNeutral.setSummary("Current value is '" + mPrefTiltNeutral.getEntry()+"'");
	        }
		    else if(key.equals(PrefsHelper.PREF_EMU_SOUND))
		    {
		    	mPrefSound.setSummary("Current value is '" + mPrefSound.getEntry()+"'");
	        }
		    else if(key.equals(PrefsHelper.PREF_STICK_TYPE))
		    {
		    	mPrefStickType.setSummary("Current value is '" + mPrefStickType.getEntry()+"'");
		    }
		    else if(key.equals(PrefsHelper.PREF_NUMBUTTONS))
		    {
		    	mPrefNumButtons.setSummary("Current value is '" + mPrefNumButtons.getEntry()+"'");
		    }
		    else if(key.equals(PrefsHelper.PREF_BUTTONS_SIZE))
		    {
		    	mPrefSizeButtons.setSummary("Current value is '" + mPrefSizeButtons.getEntry()+"'");
		    }
			else if(key.equals(PrefsHelper.PREF_BUTTONS_ALPHA))
			{
				mPrefAlphaButtons.setSummary("Current value is '" + mPrefAlphaButtons.getEntry()+"'");
			}
		    else if(key.equals(PrefsHelper.PREF_STICK_SIZE))
		    {
		    	mPrefSizeStick.setSummary("Current value is '" + mPrefSizeStick.getEntry()+"'");
		    }
			else if(key.equals(PrefsHelper.PREF_MAIN_THREAD_PRIORITY))
			{
	            mPrefMainThPr.setSummary("Current value is '" + mPrefMainThPr.getEntry()+"'");
			}
		    else if(key.equals(PrefsHelper.PREF_SOUND_ENGINE))
		    {
	            mPrefSoundEngine.setSummary("Current value is '" + mPrefSoundEngine.getEntry()+"'");
		    }
		    else if(key.equals(PrefsHelper.PREF_GLOBAL_NAVBAR_MODE))
		    {
		    	mPrefNavbar.setSummary("Current value is '" + mPrefNavbar.getEntry()+"'");
		    }
		    else if(key.equals(PrefsHelper.PREF_INSTALLATION_DIR))
		    {
		    	mPrefInstPath.setSummary("Current value is '" + mPrefInstPath.getText()+"'");
		    }
			else if(key.equals(PrefsHelper.PREF_SHADER_EFFECT))
			{
				mPrefShader.setSummary("Current value is '" + mPrefShader.getEntry()+"'");
			}
			else if(key.equals(PrefsHelper.PREF_SHADERS_ENABLED))
			{
				SharedPreferences.Editor edit = sharedPreferences.edit();
				boolean enable = sharedPreferences.getBoolean(PrefsHelper.PREF_SHADERS_ENABLED,false);

				android.app.UiModeManager u = (android.app.UiModeManager) this.getSystemService(Context.UI_MODE_SERVICE);
				boolean androidTv  = u.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;

				if(enable){
					edit.putString(PrefsHelper.PREF_EMU_RESOLUTION, "0");
					//if(!androidTv)
					  //edit.putString(PrefsHelper.PREF_EMU_RESOLUTION_OSD, "0");
				}
				else{
					edit.putString(PrefsHelper.PREF_EMU_RESOLUTION, "1");
					//if(!androidTv)
					   //edit.putString(PrefsHelper.PREF_EMU_RESOLUTION_OSD, "1");
				}
				edit.commit();
			}
			else if(key.equals(PrefsHelper.PREF_SCRAPE_ICONS) ||
				key.equals(PrefsHelper.PREF_SCRAPE_SNAPSHOTS) ||
				key.equals(PrefsHelper.PREF_SCRAPE_ALL)){
				ScraperHelper.reset();
			}
			else if(key.equals(PrefsHelper.PREF_EMU_NUM_PROCESSORS))
			{
				mPrefNumProcessors.setSummary("Current value is '" + mPrefNumProcessors.getEntry()+"'");
			}

	    }

		@Override
		public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
				Preference pref) {

			if (pref.getKey().equals("defineKeys")) {
				startActivityForResult(new Intent(this, DefineKeys.class), 1);
			}
			else if (pref.getKey().equals("changeRomPath")) {
				 AlertDialog.Builder builder = new AlertDialog.Builder(this);
			    	builder.setMessage("Are you sure? (app restart needed)")
		    	       .setCancelable(false)
		    	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		    	           public void onClick(DialogInterface dialog, int id) {
		    					SharedPreferences.Editor editor =  settings.edit();
		    					editor.putString(PrefsHelper.PREF_ROMsDIR, null);
		    					editor.commit();
		    					Emulator.setNeedRestart(true);
		    	                //android.os.Process.killProcess(android.os.Process.myPid());
		    	           }
		    	       })
		    	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		    	           public void onClick(DialogInterface dialog, int id) {
		    	                dialog.cancel();
		    	           }
		    	       });
			    	Dialog dialog = builder.create();
			    	dialog.show();
			}
			else if (pref.getKey().equals("defaultsKeys")) {

				 AlertDialog.Builder builder = new AlertDialog.Builder(this);
			    	builder.setMessage("Are you sure to restore?")
		    	       .setCancelable(false)
		    	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		    	           public void onClick(DialogInterface dialog, int id) {
		    					SharedPreferences.Editor editor =  settings.edit();

		    					StringBuffer definedKeysStr = new StringBuffer();

		    					for(int i=0; i< GameController.defaultKeyMapping.length;i++)
		    					{
									GameController.keyMapping[i] = GameController.defaultKeyMapping[i];
		    						definedKeysStr.append(GameController.defaultKeyMapping[i]+":");
		    					}
		    					editor.putString(PrefsHelper.PREF_DEFINED_KEYS, definedKeysStr.toString());
		    					editor.commit();
		    					//finish();

		    	           }
		    	       })
		    	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		    	           public void onClick(DialogInterface dialog, int id) {
		    	                dialog.cancel();
		    	           }
		    	       });
			    	Dialog dialog = builder.create();
			    	dialog.show();
			}
			else if (pref.getKey().equals("customControlLayout")) {
				ControlCustomizer.setEnabled(true);
				finish();
			}
			else if (pref.getKey().equals("defaultControlLayout")) {

				 AlertDialog.Builder builder = new AlertDialog.Builder(this);
			    	builder.setMessage("Are you sure to restore?")
		    	       .setCancelable(false)
		    	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		    	           public void onClick(DialogInterface dialog, int id) {
		    					SharedPreferences.Editor editor =  settings.edit();
		    					editor.putString(PrefsHelper.PREF_DEFINED_CONTROL_LAYOUT, null);
		    					editor.putString(PrefsHelper.PREF_DEFINED_CONTROL_LAYOUT_P, null);
		    					editor.commit();
		    	           }
		    	       })
		    	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		    	           public void onClick(DialogInterface dialog, int id) {
		    	                dialog.cancel();
		    	           }
		    	       });
			    	Dialog dialog = builder.create();
			    	dialog.show();
			}
			else if (pref.getKey().equals("defaultData")) {

				 AlertDialog.Builder builder = new AlertDialog.Builder(this);
			    	builder.setMessage("Are you sure to restore? This will remove all your MAME cfg and nvram files. This is useful to restore games to defaults to fixup mame key mappings.")
		    	       .setCancelable(false)
		    	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		    	           public void onClick(DialogInterface dialog, int id) {
		    	        	SharedPreferences.Editor editor =  settings.edit();
		    	       		editor.putBoolean(PrefsHelper.PREF_MAME_DEFAULTS, true);
		    	    		editor.commit();
		    	    		Emulator.setNeedRestart(true);
		    	           }
		    	       })
		    	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		    	           public void onClick(DialogInterface dialog, int id) {
		    	                dialog.cancel();
		    	           }
		    	       });
			    	Dialog dialog = builder.create();
			    	dialog.show();
			}

			return super.onPreferenceTreeClick(preferenceScreen, pref);
		}

		@Override
		protected void onActivityResult(int requestCode, int resultCode, Intent data) {
			super.onActivityResult(requestCode, resultCode, data);

			if (resultCode == RESULT_OK && requestCode == 0) {
				setResult(RESULT_OK, data);
			}
			else if (requestCode == 1) {
				SharedPreferences.Editor editor =  settings.edit();

				StringBuffer definedKeysStr = new StringBuffer();

				for(int i=0; i< GameController.keyMapping.length;i++)
					definedKeysStr.append(GameController.keyMapping[i]+":");

				editor.putString(PrefsHelper.PREF_DEFINED_KEYS, definedKeysStr.toString());
				editor.commit();
				return;
			}
			finish();
		}

		protected void populateShaders(ListPreference lp){

			CharSequence[] cs = null;
			CharSequence[] csv = null;

			MainHelper mh = new MainHelper(null);

			String path = getPreferenceScreen().getSharedPreferences().getString(
				PrefsHelper.PREF_INSTALLATION_DIR, "");

			ArrayList<ArrayList<String>> data = mh.readShaderCfg(path);

			int n = data.size();

			cs = new String[n + 1];
			csv = new String[n + 1];

			cs[0] = "No effect";
			csv[0] = "-1";

			int i = 0;
			while (i < n) {
				ArrayList<String> s = data.get(i);
				if(s.size() >= 2) {
					csv[i + 1] = s.get(0);
					cs[i + 1] = s.get(1);
				}
				i++;
			}
			lp.setEntries(cs);
			lp.setEntryValues(csv);
		}

	private void enableFullScreen() {
		// Window와 DecorView가 준비되었는지 확인
		if (getWindow() == null || getWindow().getDecorView() == null) {
			return;
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			// API 30 이상 (Android 11+) - 상단바와 네비게이션 바 모두 숨김
			try {
				getWindow().setDecorFitsSystemWindows(false);
				if (getWindow().getInsetsController() != null) {
					getWindow().getInsetsController().hide(
							WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
					getWindow().getInsetsController().setSystemBarsBehavior(
							WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
				}
			} catch (Exception e) {
				// API 30 방식이 실패하면 구버전 방식으로 fallback
				enableFullScreenLegacy();
			}
		} else {
			enableFullScreenLegacy();
		}

		// API 28 이상에서 디스플레이 컷아웃 모드 설정
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			try {
				WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
				layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
				getWindow().setAttributes(layoutParams);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// ★★★ 구버전 전체화면 모드 ★★★
	private void enableFullScreenLegacy() {
		// API 19-29 (구버전 방식) - 상단바와 네비게이션 바 모두 숨김
		getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
						| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
						| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_FULLSCREEN
						| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			enableFullScreen();
		}
	}
}
