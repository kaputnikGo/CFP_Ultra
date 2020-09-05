package com.cityfreqs.cfp_ultra;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.BounceInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cityfreqs.cfp_ultra.util.AudioChecker;
import com.cityfreqs.cfp_ultra.util.AudioSettings;

import java.util.Arrays;

import static com.cityfreqs.cfp_ultra.util.AudioSettings.AUDIO_BUNDLE_KEYS;
import static com.cityfreqs.cfp_ultra.util.AudioSettings.AUDIO_CHANNEL_IN;
import static com.cityfreqs.cfp_ultra.util.AudioSettings.AUDIO_ENCODING;
import static com.cityfreqs.cfp_ultra.util.AudioSettings.AUDIO_SOURCE;
import static com.cityfreqs.cfp_ultra.util.AudioSettings.MODE;

public class MainActivity extends Activity {
	private static final String TAG = "CFP_Ultra";
	
	public static boolean SCANNING = false;
	public static boolean OUTPUT = false;
	public static final boolean DEBUG = true;
	
	private static TextView debugText;
	private AudioManager audioManager;
	private static String colourString;
	//
	private ImageView bgOne;
	private ImageView bgTwo;
	private ValueAnimator bgAnim;
	private ImageView animView;
	private TranslateAnimation animation1;
	private int displayHeight;
	private boolean ANIMATING = false;
	
	private UltraService ultraService;
	private Handler handlerU;
	// service runner ms delay
	private static final int SHORT_DELAY = 10; // debug says RecordTask takes .003 to do A-Za
	private static final int LONG_DELAY = 1000;
	private int runningDelay = LONG_DELAY;

	private static int displayCounter;
	private static boolean serviceCalledStop;

	private Bundle audioBundle;
	private AlertDialog.Builder dialogBuilder;
	private AlertDialog alertDialog;
	private String[] freqSteps;
	private String[] freqRanges;
	private String[] windowTypes;
	private String[] dbLevel;
	private String[] modeSettings;

/*************************************************************************************/	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		displayHeight = size.y;
		
		
		audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		
		debugText = findViewById(R.id.debug_text);
		debugText.setMovementMethod(new ScrollingMovementMethod());
		debugText.setOnClickListener(new TextView.OnClickListener() {
			@Override
			public void onClick(View v) {
				debugText.setGravity(Gravity.NO_GRAVITY);
				debugText.setSoundEffectsEnabled(false); // no further click sounds
			}
			
		});		
		initUltra();
		
		// background animation
		bgOne = findViewById(R.id.bg_one);
		bgTwo = findViewById(R.id.bg_two);
		// foreground animation
		animView = findViewById(R.id.imageView1);
		animView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				toggleAnimation();
				toggleScanning();
			}
		});
		
		initAnimations();		
	}
	
	@Override
	protected void onResume() {
	  super.onResume();
	}
	
	@Override
	protected void onPause() {
	  super.onPause();
	}

	@Override
	protected void onDestroy() {
	  super.onDestroy();
	  stopService();
	}	

/*************************************************************************************/	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch(item.getItemId()) {
			case R.id.action_audio_scan_settings:
				changeAudioScanSettings();
				return true;
			case R.id.action_audio_scan_range:
				changeAudioScanRange();
				return true;
			case R.id.action_sensitivity_settings:
				changeSensitivitySettings();
				return true;
			case R.id.action_fft_window_settings:
				changeFFTWindowSettings();
				return true;
			case R.id.action_mode_settings:
				changeModeSettings();
				return true;
			case R.id.action_bundle_print:
				bundlePrint();
				return true;
			default:
				// do not consume the action
				return super.onOptionsItemSelected(item);
		}
	}	
	
/********************************************************************/
	
	private void initUltra() {
		populateMenuItems();

		audioBundle = new Bundle();
		audioBundle.putInt(AUDIO_BUNDLE_KEYS[11], AudioSettings.DEFAULT_FREQ_STEP);
		audioBundle.putDouble(AUDIO_BUNDLE_KEYS[12], AudioSettings.DEFAULT_MAGNITUDE);
		audioBundle.putInt(AUDIO_BUNDLE_KEYS[13], AudioSettings.DEFAULT_WINDOW_TYPE);
		audioBundle.putBoolean(AUDIO_BUNDLE_KEYS[14], false); //write audio files not applicable
		audioBundle.putBoolean(AUDIO_BUNDLE_KEYS[16], DEBUG); //set DEBUG
		audioBundle.putInt(AUDIO_BUNDLE_KEYS[17], AudioSettings.DEFAULT_MODE); //set MODE
		// set NUHF defaults
		audioBundle.putInt(AUDIO_BUNDLE_KEYS[9], AudioSettings.DEFAULT_FREQUENCY_MIN);
		audioBundle.putInt(AUDIO_BUNDLE_KEYS[10], AudioSettings.DEFAULT_FREQUENCY_MAX);

		AudioChecker audioChecker = new AudioChecker(audioBundle);

		if (audioChecker.determineRecordAudioType()) {
			logger(TAG, getString(R.string.audio_check_pre_1));
			logger(TAG, getAudioCheckerReport());

			toggleHeadset();
			handlerU = new Handler();
			colourString = "A";
			displayCounter = 0;
			serviceCalledStop = false;
		}
		else {
			logger(TAG, "Failed to init audio device.");
			colourString = "a";
		}
	}

	private String getAudioCheckerReport() {
		return ("audio record format: "
				+ audioBundle.getInt(AUDIO_BUNDLE_KEYS[1]) +
				" Hz, " + audioBundle.getInt(AUDIO_BUNDLE_KEYS[4]) +
				" ms, " + AUDIO_ENCODING[audioBundle.getInt(AUDIO_BUNDLE_KEYS[3])] +
				", " + AUDIO_CHANNEL_IN[audioBundle.getInt(AUDIO_BUNDLE_KEYS[2])] +
				", " + AUDIO_SOURCE[audioBundle.getInt(AUDIO_BUNDLE_KEYS[0])]);
	}
	
	private void initAnimations() {
		// background
		bgAnim = ValueAnimator.ofFloat(0.0f, 1.0f);
		bgAnim.setRepeatCount(ValueAnimator.INFINITE);
		bgAnim.setInterpolator(new LinearInterpolator());
		bgAnim.setDuration(10000L);
		bgAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				final float progress = (float) animation.getAnimatedValue();
				final float width = bgOne.getWidth();
				final float translationX = width * progress;
				bgOne.setTranslationX(translationX);
				bgTwo.setTranslationX(translationX - width);
			}
		});
		
		// foreground
		animation1 = new TranslateAnimation(0, 0, 0, displayHeight * 0.5f);
		animation1.setStartOffset(500);
		animation1.setDuration(3000);
		animation1.setFillAfter(true);
		animation1.setInterpolator(new BounceInterpolator());
		animation1.setRepeatMode(Animation.INFINITE);
		animation1.setRepeatCount(20);	
		animation1.setAnimationListener(new TranslateAnimation.AnimationListener() {			 
            @Override
            public void onAnimationStart(Animation animation) {
                //
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                //
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            	animView.clearAnimation();
                animView.layout(animView.getLeft(), 
                		animView.getTop(), 
                		animView.getRight(), 
                		animView.getBottom());
                ANIMATING = false;
            }
        });	
	}

	private void populateMenuItems() {
		freqSteps = new String[5];
		freqSteps[0] = getResources().getString(R.string.freq_step_10_text);
		freqSteps[1] = getResources().getString(R.string.freq_step_25_text);
		freqSteps[2] = getResources().getString(R.string.freq_step_50_text);
		freqSteps[3] = getResources().getString(R.string.freq_step_75_text);
		freqSteps[4] = getResources().getString(R.string.freq_step_100_text);

		freqRanges = new String[2];
		freqRanges[0] = getResources().getString(R.string.freq_range_one);
		freqRanges[1] = getResources().getString(R.string.freq_range_two);

		windowTypes = new String[5];
		windowTypes[0] = getResources().getString(R.string.dialog_window_fft_1);
		windowTypes[1] = getResources().getString(R.string.dialog_window_fft_2);
		windowTypes[2] = getResources().getString(R.string.dialog_window_fft_3);
		windowTypes[3] = getResources().getString(R.string.dialog_window_fft_4);
		windowTypes[4] = getResources().getString(R.string.dialog_window_fft_5);

		dbLevel = new String[7];
		dbLevel[0] = getResources().getString(R.string.magnitude_50_text);
		dbLevel[1] = getResources().getString(R.string.magnitude_60_text);
		dbLevel[2] = getResources().getString(R.string.magnitude_70_text);
		dbLevel[3] = getResources().getString(R.string.magnitude_80_text);
		dbLevel[4] = getResources().getString(R.string.magnitude_90_text);
		dbLevel[5] = getResources().getString(R.string.magnitude_93_text);
		dbLevel[6] = getResources().getString(R.string.magnitude_100_text);

		modeSettings = new String[3];
		modeSettings[0] = getResources().getString(R.string.mode_type_1);
		modeSettings[1] = getResources().getString(R.string.mode_type_2);
		modeSettings[2] = getResources().getString(R.string.mode_type_3);
	}

	/********************************************************************/

	private void changeAudioScanSettings() {
		dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setItems(freqSteps, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialogInterface, int which) {
				audioBundle.putInt(AUDIO_BUNDLE_KEYS[11], AudioSettings.FREQ_STEPS[which]);
				logger(TAG, getResources().getString(R.string.option_dialog_9) + AudioSettings.FREQ_STEPS[which]);
			}
		});

		dialogBuilder.setTitle(R.string.dialog_freq_step);
		alertDialog = dialogBuilder.create();
		alertDialog.show();
	}

	private void changeAudioScanRange() {
		dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setItems(freqRanges, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialogInterface, int which) {
				switch(which) {
					case 0:
						setFreqMinMax(1);
						logger(TAG, getResources().getString(R.string.option_dialog_10));
						break;
					case 1:
						setFreqMinMax(2);
						logger(TAG, getResources().getString(R.string.option_dialog_11));
				}
			}
		});
		dialogBuilder.setTitle(R.string.dialog_freq_range);
		alertDialog = dialogBuilder.create();
		alertDialog.show();
	}


	private void changeSensitivitySettings() {
		dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setItems(dbLevel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialogInterface, int which) {
				audioBundle.putDouble(AUDIO_BUNDLE_KEYS[12], AudioSettings.MAGNITUDES[which]);
				logger(TAG, getResources().getString(R.string.option_dialog_12) + AudioSettings.DECIBELS[which]);
			}
		});
		dialogBuilder.setTitle(R.string.dialog_sensitivity_text);
		alertDialog = dialogBuilder.create();
		alertDialog.show();
	}

	private void changeFFTWindowSettings() {
		dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setItems(windowTypes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialogInterface, int which) {
				// numerical values from 1-5
				audioBundle.putInt(AUDIO_BUNDLE_KEYS[13], which + 1);
				logger(TAG, getResources().getString(R.string.option_dialog_13) + AudioSettings.FFT_WINDOWS[which]);
			}
		});
		dialogBuilder.setTitle(R.string.dialog_window_text);
		alertDialog = dialogBuilder.create();
		alertDialog.show();
	}

	private void changeModeSettings() {
		dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setItems(modeSettings, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialogInterface, int which) {
				switch(which) {
					case 0:
						//Letter Sequence
						audioBundle.putInt(AUDIO_BUNDLE_KEYS[17], 1);
						break;
					case 1:
						// Binary Bit
						audioBundle.putInt(AUDIO_BUNDLE_KEYS[17], 2);
						break;
					case 2:
						//Clock FSK
						audioBundle.putInt(AUDIO_BUNDLE_KEYS[17], 3);
						break;
					default:
						// do nothing, catch dismisses
						break;
				}
				logger(TAG, MODE[audioBundle.getInt(AUDIO_BUNDLE_KEYS[17])]+ " mode selected.");
			}
		});
		dialogBuilder.setTitle(R.string.action_mode_settings);
		alertDialog = dialogBuilder.create();
		alertDialog.show();
	}

	private void setFreqMinMax(int pair) {
		// at the moment stick to ranges of 3kHz as DEFAULT or SECOND pair
		// use int as may get more ranges than the 2 presently used
		if (pair == 1) {
			audioBundle.putInt(AUDIO_BUNDLE_KEYS[9], AudioSettings.DEFAULT_FREQUENCY_MIN);
			audioBundle.putInt(AUDIO_BUNDLE_KEYS[10], AudioSettings.DEFAULT_FREQUENCY_MAX);
		}
		else if (pair == 2) {
			audioBundle.putInt(AUDIO_BUNDLE_KEYS[9], AudioSettings.SECOND_FREQUENCY_MIN);
			audioBundle.putInt(AUDIO_BUNDLE_KEYS[10], AudioSettings.SECOND_FREQUENCY_MAX);
		}
	}

	/********************************************************************/

	private void toggleScanning() {
		if (SCANNING) {
			SCANNING = false;
			ultraService.stopUltraService();
			ultraService = null;
		}
		else {
			// TODO audioBundle delivered here to FreqDetector and RecordTask
			ultraService = new UltraService(audioBundle);
			SCANNING = true;
			startService();
		}
	}
	
	private void toggleHeadset() {
		// if no headset, mute the audio output else feedback		
		if (OUTPUT) {
			// volume to on
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 
					audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2, 
					AudioManager.FLAG_SHOW_UI);
		}
		else {
			// volume to 0
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 
					0, 
					AudioManager.FLAG_SHOW_UI);
		}
	}
	
	private void toggleAnimation() {
		if (ANIMATING) {
			// stop it...
			bgAnim.cancel();
			animation1.cancel();
			ANIMATING = false;
		}
		else {
			bgAnim.start();
			animView.startAnimation(animation1);
			ANIMATING = true;
		}
	}

	private void bundlePrint() {
		// debug printout to check vars in audioBundle
		if (audioBundle == null) {
			logger(TAG,"audioBundle not found.");
		}
		Bundle bundle = new Bundle();
		bundle.putAll(audioBundle);
		logger(TAG, bundle.toString());
	}

/********************************************************************/
		
	public static void alphaDelivery(String delivery) {
		// receive a letter from the service
		// TODO Twitch test gets to G with mid output
		logger(TAG, "alpha delivery: " + delivery);
		colourString = delivery;
		if (colourString.compareTo("A") == 0) {
			displayCounter = 1;
		}
		else if (colourString.compareTo("G") == 0) {
			displayCounter = 3;
		}
		else if (colourString.compareTo("N") == 0) {
			displayCounter = 5;
		}
		else if (colourString.compareTo("U") == 0) {
			displayCounter = 7;
		}
	}
	
	private void colourDelivery() {
		if (displayCounter == 1) {
			toasterMessage("A nearby IoT device is signalling...");
			displayCounter = 2;
		}
		else if (displayCounter == 3) {
			toasterMessage("Receiving instructions...");
			displayCounter = 4;
		}
		else if (displayCounter == 5) {
			toasterMessage("Connecting to internet, sending data...");
			displayCounter = 6;
		}
		else if (displayCounter == 7) {
			toasterMessage("Finished.");
			displayCounter = 8;
			// TODO call to stop service here
			invokeWebsite();
		}	
	}
	
	private void toasterMessage(String message) {
		Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
		toast.show();
	}
	
	private void invokeWebsite() {
		// load up device default browser with webpage,
		// TODO add some PII vars from device to print to webpage  too
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://akm.net.au/pilfershush/index.html"));
		startActivity(browserIntent);
	}

/********************************************************************/

public static void binaryDelivery(String[] delivery) {
	// receive possible binary bit String[] from the service
	// or a null
	if (delivery == null) {
		Log.d(TAG, "Received a null delivery array from UltraService.");
	}
	else {
		// should have a String[16] of 1s and 0s
		logger(TAG, "binary delivery: " + Arrays.toString(delivery));
		// stop the service if running
		serviceCalledStop = true;
	}
}

/********************************************************************/		
	
	// currently using A-Z at 75 Hz gap.
	// transmitted with 1000-500ms gap between each?	
	private Runnable serviceRunner = new Runnable() {
		@Override
		public void run() {
			try {				
				if (ultraService != null) {
					if (ultraService.alphaSequence) {
						// allow UltraService to continuously record for the sequence
						colourDelivery();
						runningDelay = SHORT_DELAY;
					}
					else if (ultraService.binarySequence) {
						runningDelay = SHORT_DELAY;
					}
					else {
						runningDelay = LONG_DELAY;
					}
					ultraService.runUltraService();
				}
			}
			finally {
				handlerU.postDelayed(serviceRunner, runningDelay);
			}
		}
	};
	
	private void startService() {
		logger(TAG, "Service Runner call.");
		serviceRunner.run();		
	}
	
	private void stopService() {
		if (ultraService != null) {
			handlerU.removeCallbacks(serviceRunner);
			if (audioBundle.getInt(AUDIO_BUNDLE_KEYS[17]) == 2) {
				// BinaryBit mode
				Log.d(TAG, "Manually stopping service.");
				ultraService.endBinaryScan();
			}
			ultraService.stopUltraService();
		}
		SCANNING = false;
	}

	
/********************************************************************/
	
    public static void logger(String tag, String message) {
    	if (DEBUG) {
    		debugText.append("\n" + tag + ": " + message);
    		Log.d(tag, message);
    	} 
    }
}
