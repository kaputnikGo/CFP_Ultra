package com.cityfreqs.cfp_ultra;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.cityfreqs.cfp_ultra.scan.FreqDetector;
import com.cityfreqs.cfp_ultra.scan.FreqDetector.RecordTaskListener;
import com.cityfreqs.cfp_ultra.scan.ProcessAlphaMode;
import com.cityfreqs.cfp_ultra.scan.ProcessBinaryMode;
import com.cityfreqs.cfp_ultra.scan.ProcessClockFSKMode;
import com.cityfreqs.cfp_ultra.util.AudioSettings;

import static com.cityfreqs.cfp_ultra.util.AudioSettings.AUDIO_BUNDLE_KEYS;

public class UltraService extends Service {
	private static final String TAG = "UltraService";
	private ProcessAlphaMode processAlphaMode;
	private ProcessBinaryMode processBinaryMode;
	private ProcessClockFSKMode processClockFSKMode;
	private FreqDetector freqDetector;

	private int mode = AudioSettings.DEFAULT_MODE;
	public boolean alphaSequence = false;
	public boolean binarySequence = false;

	public UltraService() {
		// default no args constructor
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}


	public UltraService(Bundle audioBundle) {
		mode = audioBundle.getInt(AUDIO_BUNDLE_KEYS[17], 1);
		freqDetector = new FreqDetector(audioBundle);
		freqDetector.init();
		switch (mode) {
			case 1:
				processAlphaMode = new ProcessAlphaMode(audioBundle);
				break;
			case 2:
				processBinaryMode = new ProcessBinaryMode(audioBundle);
				break;
			case 3:
				processClockFSKMode = new ProcessClockFSKMode(audioBundle);
				break;
			default:
				processAlphaMode = new ProcessAlphaMode(audioBundle);
		}
	}
	
	public void runUltraService() {
        if (MainActivity.SCANNING) {
			freqDetector.startRecording(new RecordTaskListener() {											
			
				public void onSuccess(int val) {
					// alpha mode is legacy
					if (mode == 1) {
						String str = processAlphaMode.getCharFromFrequency(val);
						if ((str != null) && (!str.equals(""))) {
							alphaSequence = processAlphaMode.SEQUENCING;
							MainActivity.payloadDelivery(str);
						}
					}
					else if (mode == 2) {
						// mode is binary
						binarySequence = processBinaryMode.addFrequency(val);
					}
					else {
						// mode is clock FSK
						processClockFSKMode.addFrequency(val);
					}
				}
						
				public void onFailure(String paramString) {
					MainActivity.logger(TAG, "Service run failed: " + paramString);
				}
			});				
        }       
	}

	// need a programmatic end to service after sequence has been found

	// called by MainActivity on touch
	public void stopUltraService() {
		try {
			freqDetector.stopRecording();
			if (mode == 1) {
				alphaSequence = false;
				processAlphaMode.resetSequences();
			}
			else if (mode == 2) {
				//
				binarySequence = false;
				processBinaryMode.resetSequences();
			}
			else {
				//
			}
		} 
		catch (Exception ex) {
			MainActivity.logger(TAG, "Stop scanning failed.");
		}
	}
}
