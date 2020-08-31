package com.cityfreqs.cfp_ultra.scan;

import android.media.AudioRecord;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.cityfreqs.cfp_ultra.scan.FreqDetector.RecordTaskListener;
import com.cityfreqs.cfp_ultra.util.AudioSettings;

import static android.os.Process.THREAD_PRIORITY_AUDIO;
import static com.cityfreqs.cfp_ultra.util.AudioSettings.PI2;
import static com.cityfreqs.cfp_ultra.util.AudioSettings.PI4;
import static com.cityfreqs.cfp_ultra.util.AudioSettings.PI6;

public class RecordTask extends AsyncTask<Void, Integer, String> {
	private static final String TAG = "RecordTask";
	private short[] bufferArray;
	private RecordTaskListener recordTaskListener;

	private Bundle audioBundle;
	private AudioRecord audioRecord;
	private int bufferReadSize;
	
	public RecordTask(Bundle audioBundle) {
		this.audioBundle = audioBundle;
		bufferArray = new short[audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[4])];
		if (audioRecord == null) {
			try {
				audioRecord = new AudioRecord(
						audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[0]),
						audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[1]),
						audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[2]),
						audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[3]),
						audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[4]));
				logger("constructor create audioRecord.");
			}
			catch (Exception ex) {
				ex.printStackTrace();
				logger("New audioRecord failed.");
			}
		}
	}
	
	public void setOnResultsListener(RecordTaskListener recordTaskListener) {
		this.recordTaskListener = recordTaskListener;
	}
	
/********************************************************************/	
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}
	
	@Override
	protected void onProgressUpdate(Integer... paramArgs) {
		if (recordTaskListener == null) {
			logger("onProgress listener null.");
			return;
		}

		if (paramArgs[0] != null) {
			recordTaskListener.onSuccess(paramArgs[0]);
		}
		else {
			recordTaskListener.onFailure("RecordTaskListener failed, no params.");
			logger("listener onFailure.");
		}
	}
	
	@Override
	protected String doInBackground(Void... paramArgs) {
		android.os.Process.setThreadPriority(THREAD_PRIORITY_AUDIO);
		if (isCancelled()) {
			// check
			logger("isCancelled check");
			return "isCancelled()";
		}
		// check audioRecord object first
		assert audioRecord != null;
		if ((audioRecord.getState() == AudioRecord.STATE_INITIALIZED)) {
			try {
				audioRecord.startRecording();
				logger("audioRecord started...");
				audioRecord.setPositionNotificationPeriod(audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[4]));// / 2);
				audioRecord.setRecordPositionUpdateListener(new AudioRecord.OnRecordPositionUpdateListener() {
					public void onMarkerReached(AudioRecord audioRecord) {
						logger("marker reached");
					}
						
					public void onPeriodicNotification(AudioRecord audioRecord) {
						magnitudeScan(audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[13]));
					}
				});

				// check for a stop
				do {
					bufferReadSize = audioRecord.read(bufferArray, 0, audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[4]));
				} while (!isCancelled());
			}
			catch (IllegalStateException exState) {
				exState.printStackTrace();
				logger("AudioRecord start recording failed.");
			}
		}
		return "RecordTask finished";
	}
	
	@Override
	protected void onPostExecute(String paramString) {
		logger("Post execute: " + paramString);
	}
	
	@Override
	protected void onCancelled() {
		logger("onCancelled called.");
		bufferReadSize = 0;
		if (audioRecord != null) {
			audioRecord.stop();
			audioRecord.release();
			logger("audioRecord stop and release.");
		}
		else {
			logger("audioRecord is null.");
		}
	}
	
/********************************************************************/	
	
	// record audio called by periodic notification (bufferSize / 2) from this service
	// checks for freqs above 18kHz that occur above threshold amplitude ( 10000 ?? )
	
	// this is first step in freqDetection, 
	// listens for any audio above minFreq and minMangnitude that *may* contain our signal
	
	private void magnitudeScan(int windowType) {
		int bufferSize;

		if (bufferReadSize > 0) {
			int i;
			bufferSize = audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[4]);
			double[] dArr = new double[bufferSize];
			for (i = 0; i < dArr.length; i++) {
				dArr[i] = bufferArray[i];
			}
			// additional rems from SDK_PROPER, default is 2
			switch (windowType) {
				case 1: // SynchronizationWrapper.SYNC_PUSH_NOTIFICATIONS_VALUE_TYPE_IOS
					for (i = 0; i < dArr.length; i++) {
                        dArr[i] = dArr[i] * (0.5d - (0.5d * Math.cos((PI2 * ((double) i)) / dArr.length)));
                    }
                    break;
				case 2: // R.styleable.com_facebook_login_view_com_facebook_logout_text
					for (i = 0; i < dArr.length; i++) {
                        dArr[i] = dArr[i] * ((0.42659d - (0.49659d * Math.cos((PI2 * 
                        		((double) i)) / dArr.length))) + (0.076849d * Math.cos((PI4 * ((double) i)) / dArr.length)));
                    }
                    break;
				case 3: // R.styleable.com_facebook_login_view_com_facebook_tooltip_mode
					for (i = 0; i < dArr.length; i++) {
                        dArr[i] = dArr[i] * (0.54d - (0.46d * Math.cos((PI2 * ((double) i)) / dArr.length)));
                    }
                    break;
				case 4: // R.styleable.com_facebook_like_view_com_facebook_auxiliary_view_position
					for (i = 0; i < dArr.length; i++) {
                        dArr[i] = dArr[i] * (((0.355768d - (0.487396d * Math.cos((PI2 * ((double) i)) / dArr.length))) + 
                        		(0.144232d * Math.cos((PI4 * ((double) i)) / dArr.length))) - 
                        		(0.012604d * Math.cos((PI6 * ((double) i)) / dArr.length)));
                    }
                    break;
			}
			
			int candidateFreq = audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[9]);
			
			// debug says RecordTask takes .003 to do A-Za
			while (candidateFreq <= audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[10])) {
				// look for any of our freqs here, increment by freqStepper
				// this will result in a found candidate for anything in our ranges...
				// also result in multiple false positives as need a length of signal?
				
				// this also means that our listener will only listen for char sequences that rise in freqStep value...
				// ie. alphabetical order...

				// set Goertzel up to look for candidate in dArr
				Goertzel goertzel = new Goertzel((float)audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[1]), (float)candidateFreq, dArr);
				goertzel.initGoertzel();
				
				// get its magnitude
				double candidateMag = goertzel.getOptimisedMagnitude();
				// set magnitude floor, raises it
				// check if above threshold
				if (candidateMag >= audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[12])) {
					publishProgress(candidateFreq);
				}								
				// next freq for loop
				candidateFreq += audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[11]);
			}				
		}
		else {
			logger("buffer 0");
		}
	}
	
	private void logger(String message) {
		Log.d(TAG, message);
	}		
}
