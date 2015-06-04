/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.calculator2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.LinearGradient;
import android.graphics.Rect;
import android.text.Editable;
import android.graphics.Shader;
import android.text.InputType;
import android.text.Selection;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.NumberKeyListener;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

/**
 * Provides vertical scrolling for the input/result EditText.
 */
@SuppressLint("NewApi")
class CalculatorDisplay extends ViewSwitcher {

	private static final String ATTR_MAX_DIGITS = "maxDigits";
	private static final int DEFAULT_MAX_DIGITS = 10;

	// only these chars are accepted from keyboard
	private static final char[] ACCEPTED_CHARS = "0123456789.+-*/\u2212\u00d7\u00f7()!%^"
			.toCharArray();

	enum Scroll {
		UP, DOWN, NONE
	}

	AnimationSet animationSet;

	TranslateAnimation inAnimUp;
	TranslateAnimation inAnimDown;

	public TextView mDisplayText;
	public TextView mHistoryText;

	private boolean mIsDisplayViewHasText;
	private Editable mPreviousText;

	TranslateAnimation outAnimUp;
	TranslateAnimation outAnimDown;

	ScaleAnimation zoomOutAnimation;

	AlphaAnimation alphaAnimation;

	private int mMaxDigits = DEFAULT_MAX_DIGITS;

	public CalculatorDisplay(Context context, AttributeSet attrs) {
		super(context, attrs);
		mMaxDigits = attrs.getAttributeIntValue(null, ATTR_MAX_DIGITS,
				DEFAULT_MAX_DIGITS);
	}

	public void setHistory(TextView text) {
		mHistoryText = text;
	}

	public void setDisplayText(TextView text) {
		mDisplayText = text;
	}

	public TextView getHistory() {
		return mHistoryText;
	}

	public TextView getDisplaytext() {
		return mDisplayText;
	}

	public int getMaxDigits() {
		return mMaxDigits;
	}

	public void setResult(String result) {
		if (result != null) {
			mHistoryText.setText(result.toString());
		}
	}

	public String getResult() {
		return mHistoryText.getText().toString();
	}

	protected void setLogic(Logic logic) {
		NumberKeyListener calculatorKeyListener = new NumberKeyListener() {
			public int getInputType() {
				return InputType.TYPE_CLASS_TEXT
						| InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
			}

			@Override
			protected char[] getAcceptedChars() {
				return ACCEPTED_CHARS;
			}

			@Override
			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend) {

				return null;
			}
		};

		Editable.Factory factory = new CalculatorEditable.Factory(logic);
		for (int i = 0; i < 2; ++i) {
			final EditText text = (EditText) getChildAt(i);
			text.setBackground(null);
			text.setEditableFactory(factory);
			text.setKeyListener(calculatorKeyListener);
			text.setSingleLine();
		}
	}

	@Override
	public void setOnKeyListener(OnKeyListener l) {
		getChildAt(0).setOnKeyListener(l);
		getChildAt(1).setOnKeyListener(l);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldW, int oldH) {

		final float scale = (float) (50.0 / 75.0);
		inAnimUp = new TranslateAnimation(0.0f, 0.0f, (float) h, 0.0f);
		inAnimUp.setDuration(220l);
		float toX = 25.0f;
		float translateHeight = (float) getHeight() + 20.0f;
		outAnimUp = new TranslateAnimation(0.0f, -0.5f, 0.0f,
				(-translateHeight / scale));
		outAnimUp.setDuration(220l);
		outAnimUp.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {
				mDisplayText.setVisibility(View.VISIBLE);
				mHistoryText.setVisibility(View.INVISIBLE);
				LinearGradient shader = new LinearGradient(0.0f, 0.0f, 0.0f,
						40.0f, getResources().getColor(
								R.color.show_screen_display_color),
						getResources().getColor(
								R.color.show_screen_history_color),
						Shader.TileMode.CLAMP);
				mDisplayText.getPaint().setShader(shader);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				if ((mIsDisplayViewHasText) && (!"0".equals(getText()))) {
					
//					mHistoryText.setText(mPreviousText.toString() + "=");
//					mHistoryText.setVisibility(View.VISIBLE);
				}
//				mDisplayText.clearAnimation();
				mDisplayText.setVisibility(View.INVISIBLE);
			
				
			}
		});
		zoomOutAnimation = new ScaleAnimation(1.0f, scale, 1.0f, scale, 1,
				1.0f, 1, 1.0f);
		zoomOutAnimation.setDuration(220l);
		animationSet = new AnimationSet(true);
		animationSet.addAnimation(outAnimUp);
		animationSet.addAnimation(zoomOutAnimation);
		animationSet.setFillAfter(true);
		inAnimDown = new TranslateAnimation(0.0f, 0.0f, (float) -h, 0.0f);
		inAnimDown.setDuration(220l);
		outAnimDown = new TranslateAnimation(0.0f, 0.0f, 0.0f, (float) h);
		outAnimDown.setDuration(220l);
	}

	void insert(String delta) {
		EditText editor = (EditText) getCurrentView();
		int cursor = editor.getSelectionStart();
		editor.getText().insert(cursor, delta);
	}

	EditText getEditText() {
		return (EditText) getCurrentView();
	}

	Editable getText() {
		EditText text = (EditText) getCurrentView();
		return text.getText();
	}

	void setText(CharSequence text, CalculatorDisplay.Scroll dir) {
		setText(text, dir, false);
	}

	void setText(CharSequence text, Scroll scroll, boolean deleteMode) {
//		Toast.makeText(getContext(), "delete mode ->" + deleteMode, Toast.LENGTH_SHORT).show();
		mPreviousText = getText();//currentview
		EditText edittext = (EditText) getNextView();// nextview
		edittext.setText(text);
		setDisplayTextString(deleteMode);
		if (mPreviousText.length() == 0){
			scroll = Scroll.NONE;
		}
			
		Editable editable1 = edittext.getText();
		boolean flag1;
		Scroll i;
		int j;
		if (!"0".equals(editable1))
			flag1 = true;
		else
			flag1 = false;
		i = Scroll.NONE;
		if (scroll == i || deleteMode) {
			if (flag1 || deleteMode) {
				setInAnimation(null);
				setOutAnimation(null);
				setHistoryEmpty();
			} else {
				mDisplayText.setText("0");
			}
		} else if(scroll == CalculatorDisplay.Scroll.UP){
			if (scroll == Scroll.UP) {
				if (flag1)
					showAnimation();
				else
					mDisplayText.setText("0");
			} else {
				TranslateAnimation translateanimation = inAnimDown;
				setInAnimation(translateanimation);
				TranslateAnimation translateanimation1 = outAnimDown;
				setOutAnimation(translateanimation1);
			}
		}
		if (mHistoryText != null
				&& (text == null || "0".equals(text)))
			mHistoryText.setText(null);
		j = text.length();
		edittext.setSelection(j);
		showNext();
	}

	private void showAnimation() {
		setInAnimation(inAnimUp);
		mDisplayText.startAnimation(animationSet);
	}

	private void setHistoryEmpty() {
		mHistoryText.setText("0");
		mHistoryText.setVisibility(View.INVISIBLE);
	}

	private void setDisplayTextString(boolean deleteModeClear) {

		mIsDisplayViewHasText = ((mPreviousText != null) && (!"0"
				.equals(mPreviousText)));
		if ((mIsDisplayViewHasText) && (!deleteModeClear)) {
			int position = mPreviousText.length();
			Selection.setSelection(mPreviousText, position);
			if (mPreviousText.length() > 14) {
				mPreviousText = (Editable) (Editable) mPreviousText
						.subSequence((mPreviousText.length() - 14),
								mPreviousText.length());
//				mPreviousText.insert(position, "...");
				mDisplayText.setText(mPreviousText + "=");
				// Toast.makeText(getContext(), mPreviousText,
				// Toast.LENGTH_SHORT).show();
				return;
			}
			mDisplayText.setText(mPreviousText + "=");
			return;
		}
		mDisplayText.setText("0");
	}

	int getSelectionStart() {
		EditText text = (EditText) getCurrentView();
		return text.getSelectionStart();
	}

	protected void onFocusChanged(boolean gain, int direction, Rect prev) {
		 if (!gain) {
		 clearFocus();
		 }
	}
}
