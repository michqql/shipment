package me.michqql.shipmentplugin.utils;

import java.io.File;
import java.util.ArrayList;

public class IOUtil {

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File makeDirectory(File parent, String folderName) {
        File folder = new File(parent, folderName);
        if(!folder.exists())
            folder.mkdirs();
        return folder;
    }

    public static File[] getFiles(File directory) {
        if(!directory.isDirectory())
            return new File[0];

        File[] files = directory.listFiles();
        if(files == null)
            return new File[0];

        ArrayList<File> result = new ArrayList<>();
        for(File f : files) {
            if(f.isFile())
                result.add(f);
        }

        return result.toArray(new File[0]);
    }
}
