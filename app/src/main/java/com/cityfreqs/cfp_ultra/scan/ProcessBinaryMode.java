package com.cityfreqs.cfp_ultra.scan;

import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;

import static com.cityfreqs.cfp_ultra.util.AudioSettings.AUDIO_BUNDLE_KEYS;

/*
- detect and parse binaryBit mode test of : 0010 0110 0110 0101
    has a carrier and bits +/- distance from carrier interspersed
    ie: carrier, 0, carrier, 0, carrier, 1, carrier, 0, ...
    this mode always carrier between valid bits
    save all as array first, then parse
 */
// binaryBit demo has total 16 binary bits plus carrier 17 bits
// total 33.
public class ProcessBinaryMode {
    private static final String TAG = "ProcessBinary";
    private static final int BINARY_BIT_DEMO_COUNT = 33;
    private static final int BINARY_BIT_DEMO_DATA_COUNT = 16;
    private ArrayList<Integer> freqInList;
    private String[] binaryPayload;
    private int freqInCounter;
    private int sentinelFreq;
    private int carrierFreq;
    private int bit0Freq;
    private int bit1Freq;
    public boolean SEQUENCING = false;
    private Bundle audioBundle;

    public ProcessBinaryMode(Bundle audioBundle) {
        this.audioBundle = audioBundle;
        freqInList = new ArrayList<>();
        binaryPayload = new String[BINARY_BIT_DEMO_COUNT];
        resetSequences();
    }

    public void resetSequences() {
        freqInList.clear();
        freqInCounter = 0;
        sentinelFreq = 0;
        carrierFreq = 0;
        bit0Freq= 0;
        bit1Freq = 0;
    }

    public boolean addFrequency(int candidateFreq) {
        if (candidateFreq < audioBundle.getInt(AUDIO_BUNDLE_KEYS[9]) || candidateFreq > audioBundle.getInt(AUDIO_BUNDLE_KEYS[10])) {
            // do something
            Log.d(TAG, "candidateFreq out of range.");
            return SEQUENCING = false;
        }
        else if (freqInCounter > BINARY_BIT_DEMO_COUNT) {
            // have the current demo test maximum
            Log.d(TAG, "Have reached BINARY_BIT_DEMO_COUNT");
            return SEQUENCING = false;
        }
        else {
            if (candidateFreq != sentinelFreq) {
                freqInList.add(candidateFreq);
                sentinelFreq = candidateFreq;
                freqInCounter++;
            }
            // still sequencing even if a dupe
            return SEQUENCING = true;
        }
    }

    public String[] processFreqList() {
        // have builtList, try and find carrier first,
        // then look for 0 below carrier of step distance
        // and 1 above carrier of step distance
        // assume carrier is first freq

        return binaryPayload;
    }

    private void detectLogic() {
        int carrierCount = 0;
        int logic0count = 0;
        int logic1count = 0;
        int unknown = 0;

        // assumes first NUHF freq is carrier
        carrierFreq = freqInList.get(0);
        carrierCount = 1;
        // try for next freq
        int nextFreq = freqInList.get(1);
        if (nextFreq > carrierFreq) {
            // assume is bit1
            bit1Freq = nextFreq;
            logic1count = 1;
        }
        else {
            // assume is bit0
            bit0Freq = nextFreq;
            logic0count = 1;
        }

        // try figure if there is the logic we expect from demo
        // count number of each in array
        for (int i = 2; i < freqInList.size(); i++) {
           if (freqInList.get(i) == bit0Freq) {
               logic0count++;
           }
           else if (freqInList.get(i) == bit1Freq) {
               logic1count++;
           }
           else if (freqInList.get(i) == carrierFreq) {
               carrierCount++;
           }
           else {
               unknown++;
           }
        }
        // total of logic0 and logic1 should be BINARY_BIT_DEMO_DATA_COUNT == 16
        int dataCount = logic0count + logic1count;
        if (dataCount == BINARY_BIT_DEMO_DATA_COUNT) {
            // assume carrier count is correct?
            Log.d(TAG, "Assuming Binary Bit Logic nominal.");
            // proceed
        }
        else {
            // check and try find the error
            Log.d(TAG, "Looking for error in finding Binary Bit Mode logic.");
            if (unknown > 0) {
                // has some unknowns, now what...
                Log.d(TAG, "found unknown freqs: " + unknown);
            }
            if ((dataCount + carrierCount) < freqInList.size()) {
                // obvs as have unknowns?
                Log.d(TAG, "freqInList: " + freqInList.size()
                        + " carrierCount: " + carrierCount
                        + " logic0count: " + logic0count
                        + " logic1count: " + logic1count);
            }
        }

        // binaryBit demo has total 16 binary bits plus carrier 17 bits
        // total 33.

    }
}
