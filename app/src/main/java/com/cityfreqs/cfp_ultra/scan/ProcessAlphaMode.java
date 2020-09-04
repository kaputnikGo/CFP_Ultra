package com.cityfreqs.cfp_ultra.scan;

import android.os.Bundle;

import com.cityfreqs.cfp_ultra.MainActivity;

import static com.cityfreqs.cfp_ultra.util.AudioSettings.ALPHABET;
import static com.cityfreqs.cfp_ultra.util.AudioSettings.AUDIO_BUNDLE_KEYS;

public class ProcessAlphaMode {
	private static final String TAG = "ProcessAlpha";
	private String tempSeq;
	private String builtSeq;
	private String startBit = String.valueOf(ALPHABET[0]);
	private String stopBit = String.valueOf(ALPHABET[25]);
	public boolean SEQUENCING = false;
	private Bundle audioBundle;

	public ProcessAlphaMode(Bundle audioBundle) {
		this.audioBundle = audioBundle;
		resetSequences();
	}

/********************************************************************/
	
	public String getCharFromFrequency(int candidateFreq) {
		String str = processSingleChar(getCorrespondingChar(candidateFreq));
		if ((str != null) && (!str.equals(""))) {
			// reset
			return str;
		}
		return null;
	}
	
	public void resetSequences() {
		tempSeq = "";
		builtSeq = "";
	}
	
/********************************************************************/	

	private String getCorrespondingChar(int freq) {
		int alphaPos = 0;
		// check for lowest value, as divide by zero
		if (freq == audioBundle.getInt(AUDIO_BUNDLE_KEYS[9])) {
			return startBit;
		}
		else {
			alphaPos = ((freq - audioBundle.getInt(AUDIO_BUNDLE_KEYS[9]))
					/ audioBundle.getInt(AUDIO_BUNDLE_KEYS[11]));
		}
		// check for beyond alphabet in case
		if (alphaPos >= ALPHABET.length) {
			return stopBit;
		}
		else {
			// in range
			return String.valueOf(ALPHABET[alphaPos]);
		}
	}
	
	// send single char to MainActivity
	private String processSingleChar(String candidateChar) {
	    if (candidateChar == null || candidateChar.equals("")) {
	    	SEQUENCING = false;
	    	return null;
	    }
	    // START BIT
	    if (candidateChar.compareTo(startBit) == 0) {
	    	if (tempSeq.indexOf(startBit) != 0) {
	    		tempSeq += startBit;
	    		MainActivity.logger(TAG, "found startBit.");
	    	    // we have a startBit char
	    	    SEQUENCING = true;
		    	return startBit;
	    	}
	    	else {
	    	    // we have a char
	    	    SEQUENCING = true;
	    		return null;
	    	}
	    }
	    //STOP BIT
	    else if (candidateChar.compareTo(stopBit) == 0) {
	    	if (tempSeq.contains(startBit)) {
	    		tempSeq += stopBit;
		    	builtSeq = tempSeq;
		    	MainActivity.logger(TAG, "found stopBit: builtSeq is: " + builtSeq);
		    	SEQUENCING = false;
		    	tempSeq = "";
		    	return stopBit;
	    	}
	    	else {
	    		// may have received it before complete payload...
	    		return null;
	    	}
	    }
	    //PAYLOAD
	    else if (tempSeq.indexOf(startBit) == 0) {
	    	if (!tempSeq.contains(candidateChar)) {
	    		if (candidateChar.compareTo(tempSeq.substring(tempSeq.length() - 1)) > 0) {
		    		// check if alphabetical		    		
		    		tempSeq += candidateChar;
		    	    SEQUENCING = true;
		    		return candidateChar;
	    		}
	    		else {
		    		// out of alphabetical order
		    	    SEQUENCING = true;
		    		return null;
	    		}
	    	}
	    	else {
	    	    // we have dupe char?
	    	    SEQUENCING = true;
    			return null;
	    	}
	    }
	    //CATCH AND DEFAULT
	    else {
	    	if (tempSeq.length() >= ALPHABET.length) {
	    		MainActivity.logger(TAG, "tempSeq is filled.");
	    		SEQUENCING = false;
	    		return null;
	    	}
    	    // we have a char out of sequence
    	    SEQUENCING = true;
    		return null;
	    }
	}
}
