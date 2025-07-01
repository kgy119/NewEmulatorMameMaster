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

package com.seleuco.mame4droid.input;

import android.content.res.Configuration;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;

import com.seleuco.mame4droid.Emulator;
import com.seleuco.mame4droid.MAME4droid;
import com.seleuco.mame4droid.helpers.DialogHelper;
import com.seleuco.mame4droid.helpers.PrefsHelper;

public class InputHandler implements OnTouchListener, OnKeyListener {

	final static public String TAG = "InputHandler";
	final static public int PRESS_WAIT = 100;

	protected TouchController touchController = new TouchController();
	protected TouchStick touchStick = new TouchStick();
	protected TouchMouse touchMouse = new TouchMouse();
	protected TouchPointer touchPointer = new TouchPointer();
	protected TouchLightgun touchLightgun = new TouchLightgun();
	protected GameController gameController = new GameController();

	protected Mouse mouse = new Mouse();

	protected Keyboard keyboard = new Keyboard();
	protected TiltSensor tiltSensor = new TiltSensor();
    protected ControlCustomizer controlCustomizer = new ControlCustomizer();

	public TouchController getTouchController() {return touchController;}
	public TouchStick getTouchStick() {return touchStick;}
	public TouchMouse getTouchMouse() {return touchMouse;}
	public TouchPointer getTouchPointer() {return touchPointer;}
	public TouchLightgun getTouchLightgun() {return touchLightgun;}
	public Mouse getMouse() {return mouse;}
	public Keyboard getKeyboard() {return keyboard;}
	public GameController getGameController() {return gameController;}
    public TiltSensor getTiltSensor() {
        return tiltSensor;
    }
    public ControlCustomizer getControlCustomizer() {
        return controlCustomizer;
    }

    protected int[] digital_data = new int[4];

    protected MAME4droid mm = null;

    public InputHandler(MAME4droid value) {

        mm = value;
		if (mm == null) return;

		touchController.setMAME4droid(mm);
		touchStick.setMAME4droid(mm);
		touchMouse.setMAME4droid(mm);
		touchPointer.setMAME4droid(mm);
		touchLightgun.setMAME4droid(mm);
		mouse.setMAME4droid(mm);
		keyboard.setMAME4droid(mm);
		gameController.setMAME4droid(mm);
		tiltSensor.setMAME4droid(mm);
		controlCustomizer.setMAME4droid(mm);

        resetInput(true);
    }

    public void resetInput(boolean digital) {
        for (int i = 0; i < 4 * 3; i++) {
            try {
				if(digital) {
					if (i < 4) {
						digital_data[i] = 0;
						Emulator.setDigitalData(i, digital_data[i]);
					}
				}
                Emulator.setAnalogData(i, 0, 0);
            } catch (Throwable ignored) {
            }
        }

		Emulator.setMouseData(0, Emulator.MOUSE_BTN_UP, 1, -1, -1);
		Emulator.setMouseData(0, Emulator.MOUSE_BTN_UP, 2, -1, -1);
		Emulator.setMouseData(0, Emulator.MOUSE_BTN_UP, 3, -1, -1);

		Emulator.setTouchData(0, Emulator.FINGER_DOWN, -1, -1);
		Emulator.setTouchData(0, Emulator.FINGER_UP, -1, -1);

		touchStick.reset();
    }

	public void fixTiltCoin() {
		if (tiltSensor.isEnabled() && ((digital_data[0] & IController.COIN_VALUE) != 0 || (digital_data[0] & IController.START_VALUE) != 0)) {
			digital_data[0] &= ~IController.LEFT_VALUE;
			digital_data[0] &= ~IController.RIGHT_VALUE;
			digital_data[0] &= ~IController.UP_VALUE;
			digital_data[0] &= ~IController.DOWN_VALUE;
			Emulator.setAnalogData(0, 0, 0);
		}
	}

    public boolean onKey(View v, int keyCode, KeyEvent event) {
        //Log.d(TAG, "onKeyDown=" + keyCode + " " + event.getAction() + " " + event.getDisplayLabel() + " " + event.getUnicodeChar() + " " + event.getNumber());

		if(event.getSource() == InputDevice.SOURCE_MOUSE_RELATIVE)//TODO parametrize check mouse
		{
			return true;
		}

        if (ControlCustomizer.isEnabled()) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                mm.showDialog(DialogHelper.DIALOG_FINISH_CUSTOM_LAYOUT);
            }
            return true;
		}

        if(gameController.handleGameController(keyCode,event,digital_data))
		   return true;

		if(event.getDevice()!=null && (mm.getPrefsHelper().isVirtualKeyboardEnabled() || mm.getPrefsHelper().isKeyboardEnabled())) {

             if(keyboard.handleKeyboard(keyCode,event))
				 return true;
		}

        return false;
    }

	@Override
    public boolean onTouch(View v, MotionEvent event) {

        //Log.d("touch",event.getRawX()+" "+event.getX()+" "+event.getRawY()+" "+event.getY());
        if (mm == null /*|| mm.getMainHelper()==null*/) return false;

		//Log.d("Touch",v==mm.getEmuView() ? "EMUVIEW" : v==mm.getInputView() ? "INPUTVIEW": "OTRA");

        if (v == mm.getEmuView() &&
			touchController.getState() != TouchController.STATE_SHOWING_NONE) {//EMU VIEW WITH TOUCH CONTROLLER

			if(mm.getPrefsHelper().isTouchLightgun() && !Emulator.isInMenu()) {
				touchLightgun.handleTouchLightgun(v, event, digital_data);
				return true;
			}

			if(mm.getPrefsHelper().isTouchMouseEnabled()) {
				if((mm.getPrefsHelper().isTouchGameMouse() && !Emulator.isInMenu()) ||
					(!mm.getPrefsHelper().isTouchUI()  && (!Emulator.isInGame() || Emulator.isInMenu()))
				) {
						touchMouse.handleTouchMouse(v, event);
						return true;
				}
			}

			if(mm.getPrefsHelper().isTouchUI()) {
				touchPointer.handleTouchPointer(v,event);
				return true;
			}

            return false;

        } else if (v == mm.getInputView()) { //INPUTVIEW

            if (ControlCustomizer.isEnabled()) {
                controlCustomizer.handleMotion(event);
                return true;
            }

            if (touchController.isHandledStick() &&
				mm.getPrefsHelper().getControllerType() != PrefsHelper.PREF_DIGITAL_DPAD &&
				!(tiltSensor.isEnabled() && Emulator.isInGameButNotInMenu()))
                digital_data[0] = touchStick.handleMotion(event, digital_data[0]);

			boolean handled = touchController.handleTouchController(event,digital_data);

            if (!handled && mm.getPrefsHelper().isTouchLightgun() &&
				(!Emulator.isInMenu() || touchLightgun.getLightgun_pid()!=-1)
				&& !(mm.getMainHelper().getscrOrientation() == Configuration.ORIENTATION_PORTRAIT && !mm.getPrefsHelper().isPortraitFullscreen()

					)
            ) {
				touchLightgun.handleTouchLightgun(v, event,digital_data);
				return true;
			}
			else

			if(!handled && (mm.getPrefsHelper().isTouchMouseEnabled() || mm.getPrefsHelper().isTouchUI())
				&& !(mm.getMainHelper().getscrOrientation() == Configuration.ORIENTATION_PORTRAIT && !mm.getPrefsHelper().isPortraitFullscreen())
			) {
				if(mm.getPrefsHelper().isTouchMouseEnabled()) {
					if ((mm.getPrefsHelper().isTouchGameMouse() && !Emulator.isInMenu()) ||
						(!mm.getPrefsHelper().isTouchUI() && (!Emulator.isInGame() || Emulator.isInMenu()))
					) {
						touchMouse.handleTouchMouse(v, event);
						return true;
					}
				}

				if(mm.getPrefsHelper().isTouchUI()) {
					touchPointer.handleTouchPointer(v,event);
					return true;
				}
			}

            //return handled;
			return true;//capture all events so drag over is handled
        } else {//OFFSCREEN + EMUVIEW not TOUCH CONTROLLER

			if ((mm.getMainHelper().getscrOrientation() == Configuration.ORIENTATION_PORTRAIT &&
				touchController.getState() != TouchController.STATE_SHOWING_NONE)
                    ||
				(mm.getMainHelper().getscrOrientation() == Configuration.ORIENTATION_LANDSCAPE
					&& touchController.getState() != TouchController.STATE_SHOWING_NONE)) {

                if (mm.getPrefsHelper().isTouchLightgun() && !Emulator.isInMenu()) {//OFFSCREEN RELOAD
                    touchLightgun.handleTouchLightgun(v, event,digital_data);
                    return true;
                }

				if(mm.getPrefsHelper().isTouchMouseEnabled()) {
					if((mm.getPrefsHelper().isTouchGameMouse() && !Emulator.isInMenu()) ||
						(!mm.getPrefsHelper().isTouchUI()  && (!Emulator.isInGame() || Emulator.isInMenu()))
					) {
						touchMouse.handleTouchMouse(v, event);
						return true;
					}
				}

				if(mm.getPrefsHelper().isTouchUI()) {
					touchPointer.handleTouchPointer(v,event);
					return true;
				}

                return false;
            }

            mm.showDialog(DialogHelper.DIALOG_FULLSCREEN);
            return true;
        }
    }

	public boolean capturedPointerEvent(MotionEvent event){
		return mouse.handleMouse(event);
	}

    public void setInputListeners() {

        mm.getEmuView().setOnKeyListener(this);
        mm.getEmuView().setOnTouchListener(this);

        mm.getInputView().setOnTouchListener(this);
        mm.getInputView().setOnKeyListener(this);

        //mm.findViewById(R.id.EmulatorFrame).setOnTouchListener(this);
        //mm.findViewById(R.id.EmulatorFrame).setOnKeyListener(this);
    }

    public void unsetInputListeners() {
        if (mm == null)
            return;
        if (mm.getInputView() == null)
            return;
        if (mm.getEmuView() == null)
            return;

        mm.getEmuView().setOnKeyListener(null);
        mm.getEmuView().setOnTouchListener(null);

        mm.getInputView().setOnTouchListener(null);
        mm.getInputView().setOnKeyListener(null);
    }

    public boolean isHideTouchController() {
        return (keyboard.isKeyboardEnabled() && mm.getPrefsHelper().isKeyboardHideController())  ||
			gameController.isEnabled() ||
			(mouse.isEnabled() && Emulator.isInGameButNotInMenu())
			;
    }

    public boolean genericMotion(MotionEvent event) {

		//if(gameController.isEnabled()) {
			return gameController.genericMotion(event, digital_data);
		//}

        //return false;
    }

	public void dumpEvent(MotionEvent event) {
		String[] names = {"DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE",
			"POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?","10?","11?","12?"};
		StringBuilder sb = new StringBuilder();
		int action = event.getAction();
		int actionCode = action & MotionEvent.ACTION_MASK;
		sb.append("event ACTION_").append(names[actionCode]);
		if (actionCode == MotionEvent.ACTION_POINTER_DOWN
			|| actionCode == MotionEvent.ACTION_POINTER_UP) {
			sb.append("(pid ").append(
				//action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
				(action & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT);
			sb.append(")");
		}
		sb.append("[");
		for (int i = 0; i < event.getPointerCount(); i++) {
			sb.append("#").append(i);
			sb.append("(pid ").append(event.getPointerId(i));
			sb.append(")=").append((int) event.getX(i));
			sb.append(",").append((int) event.getY(i));
			if (i + 1 < event.getPointerCount())
				sb.append(";");
		}
		sb.append("]");
		//if(action != MotionEvent.ACTION_MOVE)
		Log.d(TAG, sb.toString());
	}

}
