package com.cityfreqs.cfp_ultra.scan;

import android.os.AsyncTask;
import android.os.Bundle;

import com.cityfreqs.cfp_ultra.MainActivity;

public class FreqDetector {
	private static final String TAG = "FreqDetector";
	
	private RecordTask recordTask;
	private Bundle audioBundle;
	
	
	public interface RecordTaskListener {
		void onFailure(String paramString);
		void onSuccess(int paramInt);
	}
		
	public FreqDetector(Bundle audioBundle) {
		this.audioBundle = audioBundle;
	}

/********************************************************************/

	public void init() {
		recordTask = new RecordTask(audioBundle);
	}
	
	public void startRecording(RecordTaskListener recordTaskListener) {
		if (recordTask == null) {
			MainActivity.logger(TAG, "recordTask null, create new");
			recordTask = new RecordTask(audioBundle);
		}
		startRecordTaskListener(recordTaskListener);		
	}
	
	private void startRecordTaskListener(RecordTaskListener recordTaskListener) {
		if (recordTask.getStatus() != AsyncTask.Status.RUNNING) {
			MainActivity.logger(TAG, "recordTask execute.");
			recordTask.setOnResultsListener(recordTaskListener);
			recordTask.execute();
		}
	}

	public void stopRecording() {
		if (recordTask != null) {
			recordTask.cancel(true);
			recordTask = null;
			MainActivity.logger(TAG, "Stop freqDetector.");
		}
	}
}

