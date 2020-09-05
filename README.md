# CFP_Ultra
Android near ultra-high frequency demonstration listener with web intent invoker

	 * - vers. 1.1
	 * - min API 18 - 4.3
	 * - build/target API - 10.x
	 * - s4 AOSP 4.3.1 (18) - test
	 * - s5 AOSP 10.0 (29) - dev

** TODO **
- more testing
- fix the stop touch area
- possible conform payloadDelivery() to Twitch stream bandwidth
- binaryBit and clockFSK may need carrier to signal a recording start, then process for logic
    or use mode to refine the scan to within parameters of tranmission, ie binary has carrier with step - scan in that range
- detect and parse binaryBit mode test of : 0010 0110 0110 0101
    has a carrier and bits +/- distance from carrier interspersed
    ie: carrier, 0, carrier, 0, carrier, 1, carrier, 0, ...
    

- Clock FSK mode, parse the assembly program into opcodes or similar.    
- make a release
- publish to GPlay? FDroid?

** CHANGES **
- rebuild for Android Studio 4 and gradle etc
- make proper audio settings and options
- audioBundle update to service record
- adding 3 detection modes 
- changed class name to ProcessLetterMode
- add bundlePrint
	 
Test application for researching methods of receiving and parsing NUHF audio signals.
 - Letter Sequence Mode:
 - Sequence using alphabet characters represented by an increasing NUHF frequency (each letter + step Hz).
 - Binary Bits Mode:
 - Sequence using 2 frequency bits (representing 1 and 0) with a central, carrier frequency sentinel tone.
 - Clock FSK Mode:
 - Sequence using a steady clock pulse carrier frequency with payload above or below of 8 bits separated by step Hz in between clock pulses.
 
 Originally coded in 2016, recreated git cos git is a git.

# 2020 Kaputnik Go
