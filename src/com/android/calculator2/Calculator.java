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

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ntian.nguiwidget.NTMenuAdapter;
import com.ntian.nguiwidget.NTMenuDialog;
import com.ntian.nguiwidget.NTMenuItem;
import com.ntian.nguiwidget.NTMenuOnClickListener;
import com.ntian.nguiwidget.util.NTSystemBarTintActivity;

public class Calculator extends NTSystemBarTintActivity implements
		Logic.Listener, OnClickListener {
	EventListener mListener = new EventListener();
	private CalculatorDisplay mDisplay;
	private Persist mPersist;
	private History mHistory;
	private Logic mLogic;
	private ViewPager mPager;
	private ImageView mImage;
	// private Switch mButton;

	static final int BASIC_PANEL = 0;
	static final int ADVANCED_PANEL = 1;
	private static final String LOG_TAG = "Calculator";
	private static final boolean LOG_ENABLED = true;
	private static final String STATE_CURRENT_VIEW = "state-current-view";
	private FrameLayout mPadView;
	private NTMenuDialog mPopup;
	private View mScientificPad;
	private View mSimplePad;
//	private TextView tv_his;

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		mTintManager.setStatusBarTintResource(R.color.calculator_background);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
				WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
		setContentView(R.layout.main);
		initPadView();
		mImage = (ImageView) this.findViewById(R.id.main_image);
		// mButton = (Switch) this.findViewById(R.id.main_tog);
//		tv_his = (TextView) this.findViewById(R.id.history);
//		tv_show = (TextView) this.findViewById(R.id.display_text_view);

		mImage.setOnClickListener(this);

		mPersist = new Persist(this);
		mPersist.load();
		
		mHistory = mPersist.history;
		mDisplay = (CalculatorDisplay) findViewById(R.id.display);
		mDisplay.setHistory((TextView) findViewById(R.id.history));
		mDisplay.setDisplayText((TextView) findViewById(R.id.display_text_view));
		mLogic = new Logic(this, mHistory, mDisplay);
		mLogic.setListener(this);

		mLogic.setDeleteMode(mPersist.getDeleteMode());
		mLogic.setLineLength(mDisplay.getMaxDigits());

		HistoryAdapter historyAdapter = new HistoryAdapter(this, mHistory,
				mLogic);
		mHistory.setObserver(historyAdapter);

		mListener.setHandler(mLogic, mPadView);
		mDisplay.setOnKeyListener(mListener);

		mLogic.resumeWithHistory();
		updateDeleteMode();
	}

	private void initPadView() {
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		mPadView = (FrameLayout) findViewById(R.id.pad_container);
		mPadView.removeAllViews();
		mSimplePad = View.inflate(this, R.layout.simple_pad, null);
		mPadView.removeAllViews();
		mPadView.addView(mSimplePad, layoutParams);
		mScientificPad = View.inflate(this, R.layout.advanced_pad, null);
		mPadView.addView(mScientificPad, layoutParams);
		mScientificPad.setVisibility(View.INVISIBLE);
	}

	private void updateDeleteMode() {

	}

	void setOnClickListener(View root, int id) {
		final View target = root != null ? root.findViewById(id)
				: findViewById(id);
		target.setOnClickListener(mListener);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// toggleNTMenu();
		return false;
	}

	private void toggleNTMenu() {
		// Toast.makeText(getApplicationContext(), "into",
		// Toast.LENGTH_SHORT).show();
		// if (mPopup == null) {
		// Toast.makeText(getApplicationContext(), "show",
		// Toast.LENGTH_SHORT).show();
		List<NTMenuItem> items = new ArrayList<NTMenuItem>();
		NTMenuItem item1 = new NTMenuItem(0, getResources().getString(
				R.string.clear_history));
		item1.isSelected = true;
		items.add(item1);
		NTMenuAdapter adapter = new NTMenuAdapter(this, items, itemsOnClick);
		ViewGroup mRootView = (ViewGroup) getWindow().getDecorView();
		mPopup = new NTMenuDialog(getApplicationContext());
		mPopup.setAdapter(adapter);
		mPopup.showAtLocation(mRootView, Gravity.FILL | Gravity.CENTER, 0, 0);
		mPopup.update();
		// } else {
		// Toast.makeText(getApplicationContext(), "dismiss",
		// Toast.LENGTH_SHORT).show();
		// mPopup.dismiss();
		// mPopup = null;
		// }
	}

	private NTMenuOnClickListener itemsOnClick = new NTMenuOnClickListener() {
		public void onClick(NTMenuItem v, int position) {
			// mDisplay.mDisplayText.setVisibility(View.INVISIBLE);
			// mDisplay.mHistoryText.setVisibility(View.INVISIBLE);

//			tv_his.clearAnimation();
//			tv_his.setVisibility(View.INVISIBLE);
//			tv_his.setText("");
//			tv_show.clearAnimation();
//			tv_show.setVisibility(View.INVISIBLE);
//			tv_show.setText("");
			mHistory.clear();
			mLogic.onClear();
			mPopup.dismiss();
			mPopup = null;
		}
	};

	@Override
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		if (mPager != null) {
			state.putInt(STATE_CURRENT_VIEW, mPager.getCurrentItem());
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		mLogic.updateHistory();
		mPersist.setDeleteMode(mLogic.getDeleteMode());
		mPersist.save();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mTintManager.setStatusBarDarkMode(true, this);
	}

	static void log(String message) {
		if (LOG_ENABLED) {
			Log.v(LOG_TAG, message);
		}
	}

	@Override
	public void onDeleteModeChange() {
		updateDeleteMode();
	}

	private void switchPadView() {
		final boolean toScientific = (mSimplePad.getVisibility() == 0);
		float fromX = 1.0f;
		float fromY = 1.0f;
		float toX = 1.0f;
		float toY = 1.0f;
		if (toScientific) {
			fromX = 1.25f;
			fromY = 1.2f;
			toX = toY;
		} else {
			fromX = fromY;
			toX = 1.25f;
			toY = 1.2f;
		}
		ScaleAnimation scaleAnimation = new ScaleAnimation(fromX, toX, fromY,
				toY, 1, 1.0f, 1, 1.0f);
		scaleAnimation.setDuration(500L);
		scaleAnimation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {

			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				boolean flag = toScientific;
				Calculator calculator = Calculator.this;
				calculator.showPad(flag);
				mScientificPad.clearAnimation();
			}
		});
		mScientificPad.startAnimation(scaleAnimation);
	}
	private void showPad(boolean toScientific) {
		mSimplePad.setVisibility(toScientific ? View.GONE : View.VISIBLE);
		mScientificPad.setVisibility(toScientific ? View.VISIBLE : View.GONE);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.main_image:
			switchPadView();
			break;
		default:
			break;
		}
	}
	
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
//		super.onBackPressed();
		Intent i = new Intent(Intent.ACTION_MAIN);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.addCategory(Intent.CATEGORY_HOME);
		startActivity(i);
	}
}
