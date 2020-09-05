package com.cityfreqs.cfp_ultra.scan;

import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;

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
    private int sentinelFreq;
    private int carrierFreq;
    private int bit0Freq;
    private int bit1Freq;
    private int carrierCount;
    private int logic0count;
    private int logic1count;
    private int unknown;

    public boolean SEQUENCING = false;
    private Bundle audioBundle;

    public ProcessBinaryMode(Bundle audioBundle) {
        this.audioBundle = audioBundle;
        freqInList = new ArrayList<>();
        binaryPayload = new String[BINARY_BIT_DEMO_DATA_COUNT];
        resetSequences();
    }

    public void resetSequences() {
        freqInList.clear();
        sentinelFreq = 0;
        carrierFreq = 0;
        bit0Freq= 0;
        bit1Freq = 0;
        carrierCount = 0;
        logic0count = 0;
        logic1count = 0;
        unknown = 0;
    }

    public boolean hasBinaryPayload() {
        // this serves as a stop as its polled during scanning, called from Service
        return (freqInList.size() == BINARY_BIT_DEMO_COUNT);
    }

    // TODO is stopping half way through scanning binaryBit transmission, then runs thru the detectLogic()
    // even though UI says still recording/scanning
    public boolean addFrequency(int candidateFreq) {
        if (candidateFreq < audioBundle.getInt(AUDIO_BUNDLE_KEYS[9]) || candidateFreq > audioBundle.getInt(AUDIO_BUNDLE_KEYS[10])) {
            // do something
            Log.d(TAG, "candidateFreq out of range.");
            return SEQUENCING = false;
        }
        else {
            // no freq should be same in order due to carrier
            if (candidateFreq != sentinelFreq) {
                freqInList.add(candidateFreq);
                sentinelFreq = candidateFreq;
            }
            // still sequencing even if a dupe
            return SEQUENCING = true;
        }
    }

    public String[] getBinaryPayload() {
        if (detectLogic()) {
            // can do, maybe
            Log.d(TAG, "Has detected logic, proceed.");
            // return freqInList<Integer> as a String[33]
            // has BINARY_BIT_DEMO_COUNT
            // need to strip carrier and convert to 1,0s
            if (convertLogic()) {
                int i = 0;
                for (Integer integer : freqInList) {
                    binaryPayload[i] = String.valueOf(integer);
                    i++;
                }
                // catch in case still scanning
                freqInList.clear();
                return binaryPayload;
            }
            else
                return null;
        }
        else {
            Log.d(TAG, "Could not detect logic.");
            // send an array of error message?
            return null;
        }
    }

    private boolean detectLogic() {
        // carrier and 0010 0110 0110 0101
        // assumes first NUHF freq is carrier
        // dump list to log first
        Log.d(TAG, "FreqInList: " + freqInList.toString());
        Log.d(TAG, "detect logic: " );

        carrierFreq = freqInList.get(0);
        Log.d(TAG, "Carrier freq: " + carrierFreq); // 19000
        carrierCount = 1;
        // try for next freq
        boolean foundBit1 = false;
        int nextFreq = freqInList.get(1);
        Log.d(TAG, "Nextfreq: " + nextFreq); // 18900
        if (nextFreq > carrierFreq) {
            // assume is bit1
            bit1Freq = nextFreq;
            logic1count = 1;
            foundBit1 = true;
            Log.d(TAG, "Found Bit 1 freq: " + bit1Freq);
        }
        else {
            // assume is bit0
            bit0Freq = nextFreq;
            logic0count = 1;
            Log.d(TAG, "Found Bit 0 freq: " + bit0Freq); // 18900
        }
        // dont know what the other logic/bit freq is here!!
        // look for next freq that is not carrier or bit1/bit0
        for (Integer intFreq : freqInList) {
            if (intFreq != carrierFreq) {
                if (foundBit1) {
                    if (intFreq != bit1Freq) {
                        bit0Freq = intFreq;
                        Log.d(TAG, "intFreq is bit0Freq: " + bit0Freq);
                        break;
                    }
                    // is bit1, continue
                }
                else {
                    // assume found bit 0
                    if (intFreq != bit0Freq) {
                        bit1Freq = intFreq;
                        Log.d(TAG, "intFreq is bit1Freq: " + bit1Freq); // 19100
                        break;
                    }
                    // is bit0, continue
                }
            }
            // is carrier, continue
        }

        Log.d(TAG, "have Bit0: " + bit0Freq + ", Bit1: " + bit1Freq);
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
            Log.d(TAG, "Binary Bit Data Count is good.");
            if (dataCount + carrierCount == BINARY_BIT_DEMO_COUNT) {
                Log.d(TAG, "Carrier count + Data count is good.");
                return true;
            }
            else {
                Log.d(TAG, "Carrier count + Data count is bad.");
                return false;
            }
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
            return false;
        }
    }

    private boolean convertLogic() {
        // freqInList has frequencies for carrier, logic0 and logic1
        // strip carrier freqs, replace logic freqs with 1 or 0
        // carrier is first freq, logic1 is higher, logic0 is lower than carrier
        freqInList.removeAll(Collections.singleton(carrierFreq));
        // freqInList.size should equal data count
        if (freqInList.size() == BINARY_BIT_DEMO_DATA_COUNT) {
            Log.d(TAG, "FreqInList contains only data bits.");
            // now convert highest freq to 1 and lowest freq to 0
            // using saved bit0Freq and bit1Freq
            int index = 0;
            for (Integer intFreq : freqInList) {
                if (intFreq == bit0Freq) {
                    freqInList.set(index, 0);
                }
                else if (intFreq == bit1Freq) {
                    freqInList.set(index, 1);
                }
                else {
                    // has a bad value, ignore?
                    freqInList.set(index, 8);
                    Log.d(TAG, "convertLogic found bad value, setting as 8.");
                }
                index++;
            }
            return true;
        }
        else {
            Log.d(TAG, "Error in removing carrier freq from FreqInList.");
            return false;
        }

    }
}
