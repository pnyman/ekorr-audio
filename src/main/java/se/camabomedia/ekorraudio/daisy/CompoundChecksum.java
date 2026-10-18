package se.camabomedia.ekorraudio.daisy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * CompoundChecksum
 */
public class CompoundChecksum {

  public static String checksum(String directoryPath, String fileExtension)
            throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        StringBuilder compoundChecksumBuilder = new StringBuilder();

        File directory = new File(directoryPath);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles((dir, name) -> name.endsWith(fileExtension));
            if (files != null) {
                for (File file : files) {
                    java.util.Arrays.sort(files); // stabil ordning
                    FileInputStream fis = new FileInputStream(file);
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        md.update(buffer, 0, bytesRead);
                    }
                    fis.close();

                    byte[] checksumBytes = md.digest();
                    String checksum = bytesToHex(checksumBytes);
                    compoundChecksumBuilder.append(checksum);
                }
            }
        }

        byte[] compoundChecksumBytes = md.digest(compoundChecksumBuilder.toString().getBytes());
        return bytesToHex(compoundChecksumBytes);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexBuilder = new StringBuilder();
        for (byte b : bytes) {
            hexBuilder.append(String.format("%02x", b));
        }
        return hexBuilder.toString();
    }

}
