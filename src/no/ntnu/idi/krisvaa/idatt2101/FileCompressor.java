package no.ntnu.idi.krisvaa.idatt2101;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.stream.Stream;

public class FileCompressor {}

class LempelZivCompressor extends Compressor {

    @Override
    public byte[] compress(byte[] data) throws IOException {
        byte[] compressedData = new byte[(int) (data.length * 3.5)];

        byte blockValue = 0;
        short repetitionLength = 0;

        int search_i = 0;
        int i = 0;
        int c_i = 0;

        while(i < data.length) {

            boolean foundSequence = false;
            byte bestMatchCount = 0;
            short bestBackwards = 0;

            for(blockValue = 0; (blockValue < 127) && i < data.length; blockValue++) {
                byte b = data[i];

                bestMatchCount = 0;
                bestBackwards = 0;

                for(int j = i; j > i - 32000 && j >= 0; j--) {
                    byte matchCount = 0;

                    byte tb = data[j];

                    if(b == tb) {
                        matchCount++;

                        for(int k = j; (k < i) && (i + (k - j) + 1 < data.length); k++) {
                            int i_1 = i + (k - j) + 1;
                            int i_2 = k + 1;

                            byte ftb = data[i_1];
                            byte itb = data[i_2];

                            if(ftb == itb && matchCount < 127) {
                                matchCount++;

                                if(matchCount > bestMatchCount) {
                                    bestMatchCount = matchCount;
                                    bestBackwards = (short) (i - j);
                                }
                            } else {
                                break;
                            }
                        }
                    }

                    //Mulig sekvens
                    if(bestMatchCount > 2) {
                        foundSequence = true;
                    }
                }

                if(foundSequence) {
                    break;
                }

                i++;
            }


            //Write uncompressed sequence
            if(blockValue != 0) {
                compressedData[c_i++] = (byte) (blockValue*-1);
                //System.out.println("Writing " + blockValue + " bytes with uncompressed data.");
                for(int l = blockValue; l > 0; l--) {
                    compressedData[c_i++] = data[i - l];
                }
            }

            if(foundSequence) {
                //System.out.println("Writing sequence with " + bestMatchCount + " bytes.");

                compressedData[c_i++] = (byte) (bestMatchCount);

                compressedData[c_i++] = (byte) (bestBackwards & 0xFF);

                compressedData[c_i++] = (byte) ((bestBackwards >> 8) & 0xFF);

                i+=bestMatchCount;
            }

            if(i >= data.length) break;
        }

        return Arrays.copyOf(compressedData, c_i);
    }

    @Override
    public byte[] decompress(byte[] data) {
        int expectedSize = 0;

        for(int i = 0; i < data.length;) {
            byte forwardTraversing = data[i];

            boolean noCompression = forwardTraversing < 0;

            if(noCompression) {
                i += (forwardTraversing * -1) + 1;
                expectedSize += forwardTraversing * -1;
            } else {
                i += 3;
                expectedSize += forwardTraversing;
            }
        }

        //System.out.println("Expected size after decompression: " + expectedSize);

        byte[] uncompressedData = new byte[expectedSize];
        int uncompressedDataIndex = 0;

        for(int i = 0; i < data.length; ) {
            byte forwardTraversing = data[i];

            boolean noCompression = forwardTraversing < 0;

            if(noCompression) {
                forwardTraversing *= -1;

                for(int j = 0; j < forwardTraversing; j++) {
                    byte bb = data[i+j+1];
                    uncompressedData[uncompressedDataIndex++] = bb;
                }

                i+=forwardTraversing + 1;

            } else {
                byte b_1 = data[i+1];
                byte b_2 = data[i+2];


                short backwardsTraversing = (short) ((data[i+2] << 8) | (data[i+1] & 0xFF));

                //System.out.println(b_1 + " " + b_2 + " " + backwardsTraversing + " " +uncompressedDataIndex);


                int targetIndex = (uncompressedDataIndex - backwardsTraversing + forwardTraversing);
                for(int j = uncompressedDataIndex - backwardsTraversing; j < targetIndex; j++) {
                    byte bb = uncompressedData[j];
                    uncompressedData[uncompressedDataIndex++] = bb;
                }

                i += 3;
            }
        }

        return uncompressedData;
    }

}

class HuffmannCodeCompressor extends Compressor {

    @Override
    public byte[] compress(byte[] data) throws IOException {
        int[] byteCount = new int[256];
        Arrays.fill(byteCount, 0);


        for(int i = 0; i < data.length; i++) {
            int word = data[i] + 128;
            byteCount[word]++;
        }

        for(int i = 0; i < byteCount.length; i++) {
            System.out.println((char) (i - 128) + " : " + byteCount[i]);
        }

        System.out.println(Arrays.toString(byteCount));

        return new byte[0];
    }

    @Override
    public byte[] decompress(byte[] data) {
        return new byte[0];
    }
}

abstract class Compressor {

    public abstract byte[] compress(byte[] data) throws IOException;

    public abstract byte[] decompress(byte[] data);
}
