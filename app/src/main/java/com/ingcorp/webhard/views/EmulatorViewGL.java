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

package com.ingcorp.webhard.views;

import java.util.ArrayList;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import com.ingcorp.webhard.Emulator;
import com.ingcorp.webhard.render.GLRendererES10;
import com.ingcorp.webhard.render.GLRendererES32;
import com.ingcorp.webhard.MAME4droid;
import com.ingcorp.webhard.helpers.PrefsHelper;
import com.ingcorp.webhard.render.IGLRenderer;

public class EmulatorViewGL extends GLSurfaceView implements IEmuView {

    protected int scaleType = PrefsHelper.PREF_ORIGINAL;

    protected MAME4droid mm = null;

	protected Renderer render = null;

	protected boolean showKeyboard = false;

    public Renderer getRender() {
        return render;
    }

    public int getScaleType() {
        return scaleType;
    }

	@Override
	public void showSoftKeyboard() {
		showKeyboard = true;
	}

	public void setScaleType(int scaleType) {
        this.scaleType = scaleType;
    }

    public void setMAME4droid(MAME4droid mm) {
        this.mm = mm;
		init();
		((IGLRenderer)render).setMAME4droid(mm);
    }

    public EmulatorViewGL(Context context) {
        super(context);
    }

    public EmulatorViewGL(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void init() {
        this.setKeepScreenOn(true);
        this.setFocusable(true);
        this.setFocusableInTouchMode(true);
        this.requestFocus();

		if(mm!=null) {
			if (mm.getPrefsHelper().isShadersEnabled()) {
				setEGLContextClientVersion(3);
				render = new GLRendererES32();
			} else {
				setEGLContextClientVersion(1);
				render = new GLRendererES10();
			}

			//setDebugFlags(DEBUG_CHECK_GL_ERROR | DEBUG_LOG_GL_CALLS);
			setRenderer(render);
			setRenderMode(RENDERMODE_WHEN_DIRTY);
		}
        //setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mm == null) {
            setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
        } else {
            ArrayList<Integer> l = mm.getMainHelper().measureWindow(widthMeasureSpec, heightMeasureSpec, scaleType);
            setMeasuredDimension(l.get(0).intValue(), l.get(1).intValue());
        }
        //System.out.println("onMeasure"+l.get(0).intValue()+" "+l.get(1).intValue());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Emulator.setWindowSize(w, h);
    }

	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN;
		//outAttrs.inputType = EditorInfo.TYPE_CLASS_PHONE;
		return super.onCreateInputConnection(outAttrs);
	}

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {

		if(hasWindowFocus) {

			if(mm.getPrefsHelper().isMouseEnabled())
			   this.requestPointerCapture();

			if (showKeyboard) {
				requestFocus();
				post(() -> {
					InputMethodManager imm = mm.getSystemService(InputMethodManager.class);
					boolean b = imm.showSoftInput(this, InputMethodManager.SHOW_FORCED);
					showKeyboard = !b;
				});
			}
		}
	}

	@Override
	public boolean onCapturedPointerEvent(MotionEvent motionEvent) {
        return mm.getInputHandler().capturedPointerEvent(motionEvent);
	}

}
