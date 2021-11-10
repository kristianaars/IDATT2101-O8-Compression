package no.ntnu.idi.krisvaa.idatt2101;

import java.io.*;
import java.net.URL;

public class Main {

    public static void main(String[] args) throws IOException {
        LempelZivCompressor l = new LempelZivCompressor();

        byte[] data = new URL("http://www.iie.ntnu.no/fag/_alg/kompr/diverse.txt").openStream().readAllBytes();

        //byte[] data = new FileInputStream("/Users/kristianaars/Downloads/text.text").readAllBytes();

        /*for(int i = 0; i < data.length; i++) {
            if(i % 10 == 0){
                System.out.println();
            }

            byte b = data[i];

            System.out.print((char) b);
        }*/

        System.out.println("Original size: " + data.length);

        byte[] LZcompressedData = l.compress(data);

        for(byte b : LZcompressedData) {
            //System.out.print((char) b);
        }

        System.out.println("Compressed size: " + LZcompressedData.length);


        byte[] decomrpessedData = l.decompress(LZcompressedData);
        System.out.println("Decompressed size: " + decomrpessedData.length);

        HuffmannCodeCompressor hc = new HuffmannCodeCompressor();
        hc.compress(decomrpessedData);

        saveFile("/Users/kristianaars/Downloads/div.txt.lz", LZcompressedData);
        saveFile("/Users/kristianaars/Downloads/div.txt", decomrpessedData);
    }

    public static void saveFile(String filePath, byte[] data) throws IOException {
        File f = new File(filePath);
        if(!f.exists()) {
            f.createNewFile();
        }

        FileOutputStream out = new FileOutputStream(f);

        out.write(data);
    }
}
