package com.cityfreqs.cfp_ultra.scan;

import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;

import static com.cityfreqs.cfp_ultra.util.AudioSettings.DEFAULT_FREQUENCY_MIN;
import static com.cityfreqs.cfp_ultra.util.AudioSettings.SECOND_FREQUENCY_MAX;

public class ProcessClockFSKMode {
    private static final String TAG = "ProcessClockFSK";
    private Bundle audioBundle;
    private ArrayList<Integer> freqInList;
    private ArrayList<Integer> builtList;
    private int carrierFreq;
    private int bit0Freq;
    private int bit1Freq;

    public ProcessClockFSKMode(Bundle audioBundle) {
        this.audioBundle = audioBundle;
        freqInList = new ArrayList<>();
        builtList = new ArrayList<>();
        resetSequences();
    }

    public void resetSequences() {
        freqInList.clear();
        builtList.clear();
        carrierFreq = 0;
        bit0Freq= 0;
        bit1Freq = 0;
    }

    public void addFrequency(int candidateFreq) {
        //String str = processSingleChar(getCorrespondingChar(candidateFreq));
        if(candidateFreq < DEFAULT_FREQUENCY_MIN || candidateFreq > SECOND_FREQUENCY_MAX) {
            // do something
            Log.d(TAG, "candidateFreq out of range.");
        }
        else {
            freqInList.add(candidateFreq);
        }
    }
}
