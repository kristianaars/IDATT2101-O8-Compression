package no.ntnu.idi.krisvaa.idatt2101;

import java.io.*;
import java.net.URL;
import java.util.Locale;

public class Main {

    public static void main(String[] args) throws IOException {
        if(args.length != 3) {
            System.out.println("Illegal arguments. Please provide as follows: action [compress or uncompress], infile [URL or FilePath], Outfile [FilePath]");
            return;
        }
        String action = args[0];
        String inFile = args[1];
        String outFile = args[2];

        LempelZivCompressor l = new LempelZivCompressor();
        HuffmannCodeCompressor hc = new HuffmannCodeCompressor();

        byte[] data;
        if(inFile.startsWith("https://") || inFile.startsWith("http://")) {
            data = new URL(inFile).openStream().readAllBytes();
        } else {
            data = new FileInputStream(inFile).readAllBytes();
        }

        byte[] computedData;
        if(action.toLowerCase().equals("compress")) {
            System.out.println("Original size: " + data.length);
            System.out.println("Compressing with LZ... ");
            byte[] LZcompressedData = l.compress(data);

            System.out.println("Compressed size (LZ): " + LZcompressedData.length);

            computedData = hc.compress(LZcompressedData);
            System.out.println("Compressed size (LZ+HF): " + computedData.length);
        } else if(action.toLowerCase().equals("decompress")){
            computedData = l.decompress(hc.decompress(data));
        } else {
            System.out.println("Unknown action \"" + action + "\"");
            return;
        }

        saveFile(outFile, computedData);
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
