package com.cityfreqs.cfp_ultra.scan;

import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;

import static com.cityfreqs.cfp_ultra.util.AudioSettings.DEFAULT_FREQUENCY_MIN;
import static com.cityfreqs.cfp_ultra.util.AudioSettings.SECOND_FREQUENCY_MAX;

/*
- detect and parse binaryBit mode test of : 0010 0110 0110 0101
    has a carrier and bits +/- distance from carrier interspersed
    ie: carrier, 0, carrier, 0, carrier, 1, carrier, 0, ...
    this mode always carrier between valid bits
    save all as array first, then parse
 */

public class ProcessBinaryMode {
    private static final String TAG = "ProcessBinary";
    private ArrayList<Integer> freqInList;
    private ArrayList<Integer> builtList;
    private String tempSeq;
    private String builtSeq;
    private int carrierFreq;
    private int bit0Freq;
    private int bit1Freq;
    public boolean SEQUENCING = false;
    private Bundle audioBundle;

    public ProcessBinaryMode(Bundle audioBundle) {
        this.audioBundle = audioBundle;
        freqInList = new ArrayList<>();
        builtList = new ArrayList<>();
        resetSequences();
    }

    public void resetSequences() {
        freqInList.clear();
        builtList.clear();
        tempSeq = "";
        builtSeq = "";
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

    private void removeDuplicates() {
        // weed out dupes of what is same bit
        // ie: ccccc11111ccccc00000ccccc00000
        // is c1c0c0, need to step thru array in order
        int sentinel = 0;
        for (int candy : freqInList) {
            if (candy != sentinel) {
                builtList.add(candy);
                sentinel = candy;
            }
        }
        // reached end
        // freqInList has all captures in order of receive
        // builtList has one of each unique freq in order of receive
    }

    private void processFreqList() {
        // have builtList, try and find carrier first,
        // then look for 0 below carrier of step distance
        // and 1 above carrier of step distance
        // assume carrier is first freq

    }

    private void detectLogic() {
        //
        int carrierCount = 0;
        int logic0count = 0;
        int logic1count = 0;
        int unknown = 0;
        carrierFreq = builtList.get(0);
        carrierCount = 1;
        int nextFreq = builtList.get(1);

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
        // count number of each in array
        for (int i = 2; i < builtList.size(); i++) {
           if (builtList.get(i) == bit0Freq) {
               logic0count++;
           }
           else if (builtList.get(i) == bit1Freq) {
               logic1count++;
           }
           else if (builtList.get(i) == carrierFreq) {
               carrierCount++;
           }
           else {
               unknown++;
           }
        }

        if (unknown > 0) {
            // has some unknowns, now what...
            Log.d(TAG, "found unknown freqs: " + unknown);
        }
        int totalCount = carrierCount + logic0count + logic1count;
        if (totalCount < builtList.size()) {
            // obvs as have unknowns?
            Log.d(TAG, "BuiltList: " + builtList.size()
                    + " carrierCount: " + carrierCount
                    + " logic0count: " + logic0count
                    + " logic1count: " + logic1count);
        }
        // binaryBit demo has total 16 binary bits plus carrier 17 bits
        // total 33.
    }
}
