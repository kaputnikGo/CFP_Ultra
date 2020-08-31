package com.cityfreqs.cfp_ultra.scan;

import com.cityfreqs.cfp_ultra.MainActivity;

public class ProcessAudioValue {
	private static final String TAG = "ProcessAudio";
	private String tempSeq;
	private String builtSeq;
	private int colourSeqLength;
	private String startBit;
	private String stopBit;	
	public boolean SEQUENCING = false;
	
/********************************************************************/
	
	public String getCharFromFrequency(int candidateFreq) {
		// startbit prefix and seq length could be variables...
		startBit = "A";
		stopBit = "a";
		colourSeqLength = 21;
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
		switch (freq) {
		    case 18000: 
		    	return "A";
		    case 18075: 
		    	return "B";
		    case 18150: 
		    	return "C";
		    case 18225: 
		    	return "D";
		    case 18300: 
		    	return "E";
		    case 18375: 
		    	return "F";
		    case 18450: 
		    	return "G";
		    case 18525: 
		    	return "H";
		    case 18600: 
		    	return "I";
		    case 18675: 
		    	return "J";
		    case 18750: 
		    	return "K";
		    case 18825: 
		    	return "L";
		    case 18900: 
		    	return "M";
		    case 18975: 
		    	return "N";
		    case 19050: 
		    	return "O";
		    case 19125: 
		    	return "P";
		    case 19200: 
		    	return "Q";
		    case 19275: 
		    	return "R";
		    case 19350: 
		    	return "S";
		    case 19425: 
		    	return "T";
		    case 19500: 
		    	return "U";
		    case 19575: 
		    	return "V";
		    case 19650: 
		    	return "W";
		    case 19725: 
		    	return "X";
		    case 19800: 
		    	return "Y";
		    case 19875: 
		    	return "Z";
		    case 19950:
		    	return "a";
			default: 
				return null;
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
	    	if (tempSeq.length() >= colourSeqLength) {
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
