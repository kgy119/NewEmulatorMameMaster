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

package com.seleuco.mame4droid.helpers;

import static com.seleuco.mame4droid.input.InputHandler.PRESS_WAIT;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;

import com.seleuco.mame4droid.Emulator;
import com.seleuco.mame4droid.MAME4droid;
import com.seleuco.mame4droid.input.ControlCustomizer;
import com.seleuco.mame4droid.views.IEmuView;

import java.util.Arrays;

public class DialogHelper {

	public static int savedDialog = DialogHelper.DIALOG_NONE;

	public final static int DIALOG_NONE = -1;
	public final static int DIALOG_EXIT = 1;
	public final static int DIALOG_ERROR_WRITING = 2;
	public final static int DIALOG_INFO = 3;
	public final static int DIALOG_OPTIONS = 5;
	public final static int DIALOG_FULLSCREEN = 7;
	public final static int DIALOG_FINISH_CUSTOM_LAYOUT = 10;
	public final static int DIALOG_EMU_RESTART = 11;
	public final static int DIALOG_NO_PERMISSIONS = 12;
	public final static int DIALOG_ROMs = 13;

	protected MAME4droid mm = null;

	static protected String errorMsg;
	static protected String infoMsg;

	public void setErrorMsg(String errorMsg) {
		DialogHelper.errorMsg = errorMsg;
	}

	public void setInfoMsg(String infoMsg) {
		DialogHelper.infoMsg = infoMsg;
	}

	public DialogHelper(MAME4droid value) {
		mm = value;
	}

	public Dialog createDialog(int id) {

		Dialog dialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(mm);
		switch (id) {
			case DIALOG_FINISH_CUSTOM_LAYOUT:

				builder.setMessage("Do you want to save changes?")
					.setCancelable(false)
					.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							DialogHelper.savedDialog = DIALOG_NONE;
							mm.removeDialog(DIALOG_FINISH_CUSTOM_LAYOUT);
							ControlCustomizer.setEnabled(false);
							mm.getInputHandler().getControlCustomizer().saveDefinedControlLayout();
							mm.getEmuView().setVisibility(View.VISIBLE);
							mm.getEmuView().requestFocus();
							Emulator.resume();
							mm.getInputView().invalidate();
						}
					})
					.setNegativeButton("No", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							DialogHelper.savedDialog = DIALOG_NONE;
							mm.removeDialog(DIALOG_FINISH_CUSTOM_LAYOUT);
							ControlCustomizer.setEnabled(false);
							mm.getInputHandler().getControlCustomizer().discardDefinedControlLayout();
							mm.getEmuView().setVisibility(View.VISIBLE);
							mm.getEmuView().requestFocus();
							Emulator.resume();
							mm.getInputView().invalidate();
						}
					});
				dialog = builder.create();
				break;

			case DIALOG_ROMs:

				String message = "";

				message += "Where do you have stored or want to store your ROM files?\n\n" +
					"By default, an empty internal folder will be used (but you should manually copy your ROMs files there using a PC). You can also select a folder with ROMs files from your external storage which will "
					+ "need to be authorized in the next step so MAME4droid can read the ROMS from it.\n\n" +
					"Also, if you select an external storage folder, your ROMs files will not be deleted when the app is uninstalled and you can use an Android file manager to move your ROMs there without needing a PC.";

				builder.setMessage(message)
					.setCancelable(false)
					.setPositiveButton("Internal", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							DialogHelper.savedDialog = DIALOG_NONE;
							mm.removeDialog(DIALOG_ROMs);
							if (mm.getMainHelper().isAndroidTV())
								mm.getMainHelper().setInstallationDirType(MainHelper.INSTALLATION_DIR_MEDIA_FOLDER);
							else
								mm.getMainHelper().setInstallationDirType(MainHelper.INSTALLATION_DIR_FILES_DIR);
							mm.getPrefsHelper().setROMsDIR("");
							mm.getPrefsHelper().setSAF_Uri(null);

							Thread t = new Thread(new Runnable() { public void run() {
								mm.runMAME4droid();
							}});
							t.start();

							//mm.runMAME4droid();
						}
					})
					.setNegativeButton("External Storage", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							DialogHelper.savedDialog = DIALOG_NONE;
							mm.removeDialog(DIALOG_ROMs);

							try {
								Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
								mm.startActivityForResult(intent, MainHelper.REQUEST_CODE_OPEN_DIRECTORY);
								//throw new ActivityNotFoundException("TEST");
							} catch (ActivityNotFoundException e) {

								String msg = "Your device doesn't have the native android file manager needed to authorize external storage reading.";
								if (mm.getMainHelper().isAndroidTV())
									msg += "\n\nSome Android TV devices don't include the OS document picker which is needed to grant folder permissions for the apps on Android 11+. As an alternative, select default which use the App Media Folder which is supported on all devices and is created at Android/media/[app package name]. Move any emulator files into that folder with a file manager so the app can access them without any special permissions.";

								mm.getDialogHelper().showMessage(msg,
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											android.os.Process.killProcess(android.os.Process.myPid());
										}
									});
							}
						}
					});

				dialog = builder.create();
				break;
			case DIALOG_EXIT:

				builder.setMessage("Are you sure you want to exit from app?")
					.setCancelable(false)
					.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							//System.exit(0);
							mm.finishAndRemoveTask();
							android.os.Process.killProcess(android.os.Process.myPid());
						}
					})
					.setNegativeButton("No", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Emulator.resume();
							DialogHelper.savedDialog = DIALOG_NONE;
							mm.removeDialog(DIALOG_EXIT);
							//dialog.cancel();
						}
					});
				dialog = builder.create();
				break;
			case DIALOG_ERROR_WRITING:
				builder.setMessage("Error")
					.setCancelable(false)
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							//System.exit(0);
							DialogHelper.savedDialog = DIALOG_NONE;
							mm.removeDialog(DIALOG_ERROR_WRITING);
							mm.getMainHelper().restartApp();
							//mm.showDialog(DialogHelper.DIALOG_LOAD_FILE_EXPLORER);
						}
					});

				dialog = builder.create();
				break;
			case DIALOG_INFO:
				builder.setMessage("Info")
					.setCancelable(false)
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							DialogHelper.savedDialog = DIALOG_NONE;
							Emulator.resume();
							mm.removeDialog(DIALOG_INFO);
						}
					});

				dialog = builder.create();
				break;
			case DIALOG_OPTIONS:
			case DIALOG_FULLSCREEN:
				CharSequence[] items1 = {"Load State", "Save State", "Help", "Settings", "Keyboard"};
				CharSequence[] items2 = {"Help", "Settings", "Keyboard"};
				CharSequence[] items3 = {"Exit", "Load State", "Save State", "Help", "Settings", "Keyboard"};
				CharSequence[] items4 = {"Exit", "Help", "Settings", "Keyboard"};

				boolean saveload = Emulator.isInGameButNotInMenu() && Emulator.getValue(Emulator.PAUSE)!=1;

				final int a = id == DIALOG_FULLSCREEN ? 0 : 1;
				final int b =  saveload ? 0 : 2;

				if (a == 1)
					builder.setTitle("Choose an option from the menu.");

				CharSequence[] items = saveload ? (id == DIALOG_OPTIONS ? items1 : items3) : (id == DIALOG_OPTIONS ? items2 : items4);

				boolean notKeyboard = !mm.getPrefsHelper().isVirtualKeyboardEnabled() || mm.getInputHandler().getKeyboard().isKeyboardConnected() || mm.getMainHelper().isAndroidTV();

				if(notKeyboard)
					items = Arrays.copyOf(items, items.length-1);

				builder.setCancelable(true);
				builder.setItems(items, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {

						if (item == 0 && a == 0) {

							mm.showDialog(DialogHelper.DIALOG_EXIT);

							//if (Emulator.isInMenu()) {
							    /*
								Emulator.setValue(Emulator.EXIT_GAME, 1);
								Emulator.resume();
								try {
									Thread.sleep(PRESS_WAIT);
								} catch (InterruptedException e) {
								}
								Emulator.setValue(Emulator.EXIT_GAME, 0);
								*/

							/*
							} else if (!Emulator.isInGame())
								mm.showDialog(DialogHelper.DIALOG_EXIT);
							else {
								Emulator.setValue(Emulator.EXIT_GAME, 1);
								Emulator.resume();
								try {
									Thread.sleep(PRESS_WAIT);
								} catch (InterruptedException e) {
								}
								Emulator.setValue(Emulator.EXIT_GAME, 0);
							}
							 */
						} else if (item == 1 - a && b == 0) {
							Emulator.resume();
							Emulator.setValue(Emulator.LOADSTATE, 1);
							Emulator.setSaveorload(true);
							//Emulator.resume();
							try {
								Thread.sleep(PRESS_WAIT);
							} catch (InterruptedException e) {
							}
							Emulator.setValue(Emulator.LOADSTATE, 0);
						} else if (item == 2 - a && b == 0) {
							Emulator.resume();
							Emulator.setValue(Emulator.SAVESTATE, 1);
							Emulator.setSaveorload(true);
							//Emulator.resume();
							try {
								Thread.sleep(PRESS_WAIT);
							} catch (InterruptedException e) {
							}
							Emulator.setValue(Emulator.SAVESTATE, 0);
						} else if (item == 3 - a - b) {
							mm.getMainHelper().showHelp();
						} else if (item == 4 - a - b) {
							mm.getMainHelper().showSettings();
						} else if (item == 5 - a - b) {
							((IEmuView) mm.getEmuView()).showSoftKeyboard();
							Emulator.resume();
						}
						Emulator.setInOptions(false);

						DialogHelper.savedDialog = DIALOG_NONE;
						mm.removeDialog(DIALOG_OPTIONS);
						mm.removeDialog(DIALOG_FULLSCREEN);
					}
				});
				builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {

						Emulator.resume();
						Emulator.setInOptions(false);

						DialogHelper.savedDialog = DIALOG_NONE;
						if (a != 0)
							mm.removeDialog(DIALOG_OPTIONS);
						else
							mm.removeDialog(DIALOG_FULLSCREEN);
					}
				});
				dialog = builder.create();
				break;
			case DIALOG_EMU_RESTART:
				builder.setTitle("Restart needed!")
					.setMessage("MAME4droid needs to restart for the changes to take effect.")
					.setCancelable(false)
					.setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mm.getMainHelper().restartApp();
						}
					});
				dialog = builder.create();
				break;
			case DIALOG_NO_PERMISSIONS:
				builder.setTitle("No permissions!")
					.setMessage("You don't have permission to read from external storage. Please, allow storage permission on Android applications settings.")
					.setCancelable(false)
					.setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							android.os.Process.killProcess(android.os.Process.myPid());
						}
					});
				dialog = builder.create();
				break;
			default:
				dialog = null;
		}
	    /*
	    if(dialog!=null)
	    {
	    	dialog.setCanceledOnTouchOutside(false);
	    }*/
		return dialog;

	}

	public void prepareDialog(int id, Dialog dialog) {

		if (id == DIALOG_ERROR_WRITING) {
			((AlertDialog) dialog).setMessage(errorMsg);
			DialogHelper.savedDialog = DIALOG_ERROR_WRITING;
		} else if (id == DIALOG_INFO) {
			((AlertDialog) dialog).setMessage(infoMsg);
			Emulator.pause();
			DialogHelper.savedDialog = DIALOG_INFO;
		} else if (id == DIALOG_EXIT) {
			Emulator.pause();
			DialogHelper.savedDialog = DIALOG_EXIT;
		} else if (id == DIALOG_OPTIONS) {
			Emulator.pause();
			DialogHelper.savedDialog = DIALOG_OPTIONS;
		} else if (id == DIALOG_FULLSCREEN) {
			Emulator.pause();
			DialogHelper.savedDialog = DIALOG_FULLSCREEN;
		} else if (id == DIALOG_ROMs) {
			DialogHelper.savedDialog = DIALOG_ROMs;
		} else if (id == DIALOG_FINISH_CUSTOM_LAYOUT) {
			DialogHelper.savedDialog = DIALOG_FINISH_CUSTOM_LAYOUT;
		} else if (id == DIALOG_EMU_RESTART) {
			Emulator.pause();
		} else if (id == DIALOG_NO_PERMISSIONS) {
			DialogHelper.savedDialog = DIALOG_NO_PERMISSIONS;
		}

	}

	public void removeDialogs() {
		if (savedDialog == DIALOG_FINISH_CUSTOM_LAYOUT) {
			mm.removeDialog(DIALOG_FINISH_CUSTOM_LAYOUT);
			DialogHelper.savedDialog = DIALOG_NONE;
		}
	}

	public void showMessage(String message, DialogInterface.OnClickListener okListener) {
		new AlertDialog.Builder(mm)
			.setMessage(message)
			.setCancelable(false)
			.setPositiveButton("OK", okListener)
			.create()
			.show();
	}

}
