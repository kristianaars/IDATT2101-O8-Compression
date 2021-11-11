package no.ntnu.idi.krisvaa.idatt2101;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.PriorityQueue;

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
                ////System.out.println("Writing " + blockValue + " bytes with uncompressed data.");
                for(int l = blockValue; l > 0; l--) {
                    compressedData[c_i++] = data[i - l];
                }
            }

            if(foundSequence) {
                ////System.out.println("Writing sequence with " + bestMatchCount + " bytes.");

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

        ////System.out.println("Expected size after decompression: " + expectedSize);

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

                ////System.out.println(b_1 + " " + b_2 + " " + backwardsTraversing + " " +uncompressedDataIndex);


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

        int numberOfWords = 0;

        //Count words
        for(int i = 0; i < data.length; i++) {
            int word = data[i] + 128;
            if(byteCount[word]==0) numberOfWords++;
            byteCount[word]++;
        }

        for(int i = 0; i < byteCount.length; i++) {
            //System.out.println((char) (i - 128) + " : " + byteCount[i]);
        }

        //Build huffmantree
        TreeNode root = buildHuffmanTree(byteCount);

        String[] huffmanCodeList = new String[256];
        saveCodesToTable(root, huffmanCodeList, "");

        return huffmanCompression(huffmanCodeList, data, byteCount);
    }

    @Override
    public byte[] decompress(byte[] data) {
        int dataIndex = 0;
        int decompressedDataIndex = 0;
        byte[] decompressedData = new byte[data.length * 6];

        //Read tree

        //Read and build wordCountList
        int[] wordCount = new int[256];
        for(int i = 0; i < wordCount.length; i++) {
            byte word = data[dataIndex++];
            byte[] countBytes = new byte[4];

            for(int j = 0; j < countBytes.length; j++) {
                countBytes[j] = data[dataIndex++];
            }
            int count = byteArrayToInt(countBytes);

            wordCount[word + 128] = count;
        }

        //Create tree
        TreeNode root = buildHuffmanTree(wordCount);

        String[] huffmanCodeList = new String[256];
        saveCodesToTable(root, huffmanCodeList, "");

        TreeNode currentNode = root;
        for (int i = dataIndex; i < data.length; i++) {
            byte b = data[dataIndex++];

            for(int j = 0; j < 8; j++) {
                short bin = (short) ((short) (b >> (7-j)) & 0b00000001);

                if(bin == 0) {
                    currentNode = currentNode.left;
                } else {
                    currentNode = currentNode.right;
                }

                if(currentNode instanceof LeafTreeNode) {
                    decompressedData[decompressedDataIndex++] = ((LeafTreeNode) currentNode).character;
                    currentNode = root;
                }
            }
        }

        return Arrays.copyOf(decompressedData, decompressedDataIndex);
    }

    private TreeNode buildHuffmanTree(int[] wordCount) {
        ArrayList<LeafTreeNode> words = new  ArrayList<LeafTreeNode>();
        for(int i = 0; i < wordCount.length; i++) {
            int count = wordCount[i];
            if(count > 0) {
                words.add(new LeafTreeNode((byte) (i - 128), count));
            }
        }

        Collections.sort(words, new TreeComparator());

        PriorityQueue<TreeNode> wordQueue = new PriorityQueue<>(new TreeComparator());
        wordQueue.addAll(words);

        TreeNode root = null;

        while (wordQueue.size() > 1) {
            TreeNode x = wordQueue.peek();
            wordQueue.poll();

            TreeNode y = wordQueue.peek();
            wordQueue.poll();

            //System.out.println(x.weight + " " + y.weight);

            TreeNode p = new TreeNode(x.weight + y.weight, x, y);

            root = p;

            wordQueue.add(p);
        }

        return root;
    }

    public int byteArrayToInt(byte[] bytes) {
        int r = 0;
        for(int i = 0; i < bytes.length && i < 4; i++) {
            r <<= 8;
            r |= (int)bytes[i] & 0xFF;
        }
        return r;
    }

    public static void saveCodesToTable(TreeNode root, String[] codeList, String s) {
        if (root.left == null && root.right == null && root instanceof LeafTreeNode) {
            codeList[((LeafTreeNode) root).character + 128] = s;
            return;
        }
        saveCodesToTable(root.left, codeList, s + "0");
        saveCodesToTable(root.right, codeList, s + "1");
    }


    public byte[] huffmanCompression(String[] huffmanCodeList, byte[] data, int[] wordCount) {
        byte[] compressedDataBuffer = new byte[(int) (data.length * 1.2)];
        int compressedDataIndex = 0;

        for(int i = 0; i < wordCount.length; i++) {
            byte[] wordCountBytes = ByteBuffer.allocate(4).putInt(wordCount[i]).array();
            compressedDataBuffer[compressedDataIndex++] = (byte) (i - 128);
            for(int j = 0; j < 4; j++) {
                compressedDataBuffer[compressedDataIndex++] = wordCountBytes[j];
            }
        }

        BitWriter bw = new BitWriter();

        for(int i = 0; i < data.length; i++) {
            bw.writeBits(huffmanCodeList[data[i] + 128]);
        }

        Byte[] byteList = bw.getByteList();
        for(int i = 0; i < byteList.length; i++) {
            compressedDataBuffer[compressedDataIndex++] = byteList[i];
        }

        //System.out.println(bw.length);

        return Arrays.copyOf(compressedDataBuffer, compressedDataIndex);
    }
}

class BitWriter {

    private short bufferIndex;
    private byte buffer;
    ArrayList<Byte> byteList;
    int length = 0;

    public BitWriter() {
        buffer = 0;
        byteList = new ArrayList<>();
    }

    public void writeBits(String bits) {
        length += bits.length();
        for(int i = 0; i < bits.length(); i++) {
            byte write = 0;
            if(bits.charAt(i) == '1') write = 0b00000001;
            else write = 0b00000000;

            //System.out.println(write);

            buffer = (byte) ((byte) (buffer << 1));
            buffer = (byte) (buffer | write);
            bufferIndex++;

            if(bufferIndex == 8) {
                byteList.add(buffer);
                buffer = 0;
                bufferIndex = 0;
            }
        }
    }

    public Byte[] getByteList() {
        if(bufferIndex != 0) {
            buffer = (byte) (buffer << (8 - bufferIndex));
        }

        byteList.add(buffer);

        return byteList.toArray(new Byte[0]);
    }
}

abstract class Compressor {

    public abstract byte[] compress(byte[] data) throws IOException;

    public abstract byte[] decompress(byte[] data);
}
