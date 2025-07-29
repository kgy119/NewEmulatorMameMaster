package com.ingcorp.webhard;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.ingcorp.webhard.base.Constants;
import com.ingcorp.webhard.helpers.DialogHelper;
import com.ingcorp.webhard.helpers.MainHelper;
import com.ingcorp.webhard.helpers.PrefsHelper;
import com.ingcorp.webhard.helpers.SAFHelper;
import com.ingcorp.webhard.helpers.ScraperHelper;
import com.ingcorp.webhard.input.ControlCustomizer;
import com.ingcorp.webhard.input.GameController;
import com.ingcorp.webhard.input.InputHandler;
import com.ingcorp.webhard.manager.AdMobManager;
import com.ingcorp.webhard.views.IEmuView;
import com.ingcorp.webhard.views.InputView;

public class MAME4droid extends Activity {

	private static final String TAG = Constants.LOG_TAG;

	protected View emuView = null;

	protected InputView inputView = null;

	protected MainHelper mainHelper = null;
	protected PrefsHelper prefsHelper = null;
	protected DialogHelper dialogHelper = null;
	protected SAFHelper safHelper = null;
	protected ScraperHelper scraperHelper = null;

	protected InputHandler inputHandler = null;

	public PrefsHelper getPrefsHelper() {
		return prefsHelper;
	}

	public MainHelper getMainHelper() {
		return mainHelper;
	}

	public DialogHelper getDialogHelper() {
		return dialogHelper;
	}

	public SAFHelper getSAFHelper() {
		return safHelper;
	}

	public ScraperHelper getScraperHelper() {
		return scraperHelper;
	}

	public View getEmuView() {
		return emuView;
	}

	public InputView getInputView() {
		return inputView;
	}

	public InputHandler getInputHandler() {
		return inputHandler;
	}

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		//android.os.Debug.waitForDebugger();

		Log.d(TAG, "onCreate " + this);
		System.out.println("onCreate intent:" + getIntent().getAction());

		overridePendingTransition(0, 0);
		getWindow().setWindowAnimations(0);

		prefsHelper = new PrefsHelper(this);

		dialogHelper = new DialogHelper(this);

		mainHelper = new MainHelper(this);

		safHelper = new SAFHelper(this);

		scraperHelper = new ScraperHelper(this);

		inputHandler = new InputHandler(this);

		mainHelper.detectDevice();

		inflateViews();

		Emulator.setMAME4droid(this);

		mainHelper.updateMAME4droid();

		String uri = getPrefsHelper().getSAF_Uri();
		if (uri != null) {
			safHelper.setURI(uri);
		}

		initMame4droid();

		AdMobManager.getInstance(this).loadRewardedAd(null);
	}

	protected void initMame4droid() {
		if (!Emulator.isEmulating()) {

			if (getPrefsHelper().getInstallationDIR() == null || prefsHelper.getROMsDIR() == null) {
				if (DialogHelper.savedDialog == DialogHelper.DIALOG_NONE) {
					if(getMainHelper().isAndroidTV() && getPrefsHelper().getInstallationDIR() == null ) {
						getMainHelper().setInstallationDirType(MainHelper.INSTALLATION_DIR_MEDIA_FOLDER);
						getPrefsHelper().setROMsDIR("");
						getPrefsHelper().setSAF_Uri(null);
						Thread t = new Thread(new Runnable() { public void run() { runMAME4droid();
						}});
						t.start();
					}
					else{
						getMainHelper().setInstallationDirType(MainHelper.INSTALLATION_DIR_FILES_DIR);
						getPrefsHelper().setROMsDIR("");
						getPrefsHelper().setSAF_Uri(null);

						Thread t = new Thread(new Runnable() { public void run() {
							runMAME4droid();
						}});
						t.start();
					}
				}
			} else { //roms dir no es null es que previamente hemos puesto "" o un path especifico. Venimos del recreate y si ha cambiado el installation path hay que actuzalizarlo

				boolean res = getMainHelper().ensureInstallationDIR(mainHelper.getInstallationDIR());
				if (!res) {
					this.getPrefsHelper().setInstallationDIR(this.getPrefsHelper().getOldInstallationDIR());//revert
				} else {
					runMAME4droid();//MAIN ENTRY POINT
				}
			}
		}
	}


	public void inflateViews() {

//		if (getPrefsHelper().getOrientationMode() != 0) {
//			int mode = getMainHelper().getScreenOrientation();
//			this.setRequestedOrientation(mode);
//		}

		// 항상 가로모드로 강제 설정
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

		if (getPrefsHelper().isNotchUsed() && Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
			getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
		}

		inputHandler.unsetInputListeners();

		Emulator.setPortraitFull(getPrefsHelper().isPortraitFullscreen());

		boolean full = false;
		if (prefsHelper.isPortraitFullscreen() && mainHelper.getscrOrientation() == Configuration.ORIENTATION_PORTRAIT) {
			setContentView(R.layout.main_fullscreen);
			full = true;
		} else {
			setContentView(R.layout.main);
		}

		FrameLayout fl = (FrameLayout) this.findViewById(R.id.EmulatorFrame);

		Emulator.setVideoRenderMode(getPrefsHelper().getVideoRenderMode());

		if (prefsHelper.getVideoRenderMode() == PrefsHelper.PREF_RENDER_GL) {

			if (prefsHelper.getNavBarMode() != PrefsHelper.PREF_NAVBAR_VISIBLE)
				this.getLayoutInflater().inflate(R.layout.emuview_gl_ext, fl);
			else
				this.getLayoutInflater().inflate(R.layout.emuview_gl, fl);

			emuView = this.findViewById(R.id.EmulatorViewGL);
		}

		if (full && prefsHelper.isPortraitTouchController()
		) {
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) emuView.getLayoutParams();
			lp.gravity = Gravity.TOP | Gravity.CENTER;
		}

		inputView = (InputView) this.findViewById(R.id.InputView);

		((IEmuView) emuView).setMAME4droid(this);

		inputView.setMAME4droid(this);

		View frame = this.findViewById(R.id.EmulatorFrame);
		frame.setOnTouchListener(inputHandler);

		if (!getPrefsHelper().isNotchUsed() &&  Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
			frame.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
				@Override
				public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
					Insets bars = insets.getInsets(WindowInsets.Type.displayCutout()

							/*	 | WindowInsets.Type.systemBars() */

					);
					v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
					return WindowInsets.CONSUMED;
				}
			});
		}

		inputHandler.setInputListeners();
	}

	public void runMAME4droid() {

		getMainHelper().copyFiles();
		getMainHelper().removeFiles();

		Emulator.emulate(mainHelper.getLibDir(), mainHelper.getInstallationDIR());
	}


	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.d(TAG, "방향 바뀜 onConfigurationChanged " + this);

		super.onConfigurationChanged(newConfig);

		overridePendingTransition(0, 0);

		inflateViews();

		getMainHelper().updateMAME4droid();

		overridePendingTransition(0, 0);

		AdMobManager.getInstance(this).loadRewardedAd(null);
	}


	//ACTIVITY
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (mainHelper != null)
			mainHelper.activityResult(requestCode, resultCode, data);
	}

	//LIVE CYCLE
	@Override
	protected void onResume() {
		Log.d(TAG, "onResume " + this);
		super.onResume();

		if (prefsHelper != null)
			prefsHelper.resume();

		if (DialogHelper.savedDialog != -1)
			showDialog(DialogHelper.savedDialog);
		else if (!ControlCustomizer.isEnabled())
			Emulator.resume();

		if (inputHandler != null) {
			if (inputHandler.getTiltSensor() != null)
				inputHandler.getTiltSensor().enable();
		}

		if(scraperHelper!=null){
			scraperHelper.resume();
		}

		//System.out.println("OnResume");
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause " + this);
		super.onPause();
		if (prefsHelper != null)
			prefsHelper.pause();
		if (!ControlCustomizer.isEnabled())
			Emulator.pause();
		if (inputHandler != null) {
			if (inputHandler.getTiltSensor() != null)
				inputHandler.getTiltSensor().disable();
		}

		if (dialogHelper != null) {
			dialogHelper.removeDialogs();
		}
		if(scraperHelper!=null){
			scraperHelper.pause();
		}

		//System.out.println("OnPause");
	}

	@Override
	protected void onStart() {
		Log.d(TAG, "onStart " + this);
		super.onStart();
		try {
			GameController.resetAutodetected();
		} catch (Throwable ignored) {
		}
		//System.out.println("OnStart");
	}

	@Override
	protected void onStop() {
		Log.d(TAG, "onStop " + this);
		super.onStop();
		//System.out.println("OnStop");
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.d(TAG, "onNewIntent " + this);
		System.out.println("onNewIntent action:" + intent.getAction());
		mainHelper.checkNewViewIntent(intent);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy " + this);

		View frame = this.findViewById(R.id.EmulatorFrame);
		if (frame != null)
			frame.setOnTouchListener(null);

		if (inputHandler != null) {
			inputHandler.unsetInputListeners();

			if (inputHandler.getTiltSensor() != null)
				inputHandler.getTiltSensor().disable();
		}

		if (emuView != null)
			((IEmuView) emuView).setMAME4droid(null);

		if(scraperHelper!=null){
			scraperHelper.stop();
		}

        /*
        if(inputView!=null)
           inputView.setMAME4droid(null);

        if(filterView!=null)
           filterView.setMAME4droid(null);

        prefsHelper = null;

        dialogHelper = null;

        mainHelper = null;

        inputHandler = null;

        inputView = null;

        emuView = null;

        filterView = null; */
	}


	//Dialog Stuff
	@Override
	protected Dialog onCreateDialog(int id) {

		if (dialogHelper != null) {
			Dialog d = dialogHelper.createDialog(id);
			if (d != null) return d;
		}
		return super.onCreateDialog(id);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		if (dialogHelper != null)
			dialogHelper.prepareDialog(id, dialog);
	}

	@Override
	public boolean dispatchGenericMotionEvent(MotionEvent event) {
		if (inputHandler != null)
			return inputHandler.genericMotion(event);
		return false;
	}

	@Override
	public void onPointerCaptureChanged(boolean hasCapture) {
		super.onPointerCaptureChanged(hasCapture);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		switch (requestCode) {
			case 1:
				if (grantResults == null || grantResults.length == 0) {
					//this.showDialog(DialogHelper.DIALOG_NO_PERMISSIONS);
					System.out.println("***1");
				} else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					System.out.println("***2");
					initMame4droid();
				} else {
					System.out.println("***3");
					this.showDialog(DialogHelper.DIALOG_NO_PERMISSIONS);
				}
				break;
			default:
				super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}

}