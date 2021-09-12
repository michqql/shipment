package me.michqql.shipmentplugin.utils;

import java.io.File;

public class IOUtil {

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void createFolder(File parent, String folderName) {
        File folder = new File(parent, folderName);
        if(!folder.exists())
            folder.mkdirs();
    }
}
