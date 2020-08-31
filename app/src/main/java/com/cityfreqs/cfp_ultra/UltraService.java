package com.cityfreqs.cfp_ultra;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.cityfreqs.cfp_ultra.scan.FreqDetector;
import com.cityfreqs.cfp_ultra.scan.FreqDetector.RecordTaskListener;
import com.cityfreqs.cfp_ultra.scan.ProcessAudioValue;

public class UltraService extends Service {
	private static final String TAG = "UltraService";
	private ProcessAudioValue processAudioValue;
	private FreqDetector freqDetector;
	
	public boolean sequenceDetected = false;

	public UltraService() {
		// default no args constructor
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}


	public UltraService(Bundle audioBundle) {
		freqDetector = new FreqDetector(audioBundle);
		freqDetector.init();
		processAudioValue = new ProcessAudioValue();
	}
	
	public void runUltraService() {	
        
        if (MainActivity.SCANNING) {
			freqDetector.startRecording(new RecordTaskListener() {											
			
				public void onSuccess(int val) {
					String str = processAudioValue.getCharFromFrequency(val);
					if ((str != null) && (!str.equals(""))) {
						sequenceDetected = processAudioValue.SEQUENCING;
						MainActivity.payloadDelivery(str);
					}
				}
						
				public void onFailure(String paramString) {
					MainActivity.logger(TAG, "Service run failed: " + paramString);
				}
			});				
        }       
	}
	
	public void stopUltraService() {
		try {
			freqDetector.stopRecording();
			sequenceDetected = false;
			processAudioValue.resetSequences();
		} 
		catch (Exception ex) {
			MainActivity.logger(TAG, "Stop scanning failed.");
		}
	}
}
