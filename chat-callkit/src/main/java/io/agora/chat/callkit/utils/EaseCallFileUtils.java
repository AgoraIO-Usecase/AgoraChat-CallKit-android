package io.agora.chat.callkit.utils;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class EaseCallFileUtils {

    public static String getModelFilePath(Context context, String modelName) {
        copyFileIfNeed(context,modelName);
        return context.getFilesDir().getAbsolutePath() + File.separator + modelName;
    }

    /**
     * Copy the files under the asset to the context.getFilesDir() directory
     */
    private static void copyFileIfNeed(Context context, String modelName) {
        InputStream is = null;
        OutputStream os = null;
        try {
            //By default stored in data/data/<application name>/file
            File modelFile = new File(context.getFilesDir(), modelName);
            is = context.getAssets().open(modelName);
            if (modelFile.length() == is.available()) {
                return;
            }
            os = new FileOutputStream(modelFile);
            byte[] buffer = new byte[1024];
            int length = is.read(buffer);
            while (length > 0) {
                os.write(buffer, 0, length);
                length = is.read(buffer);
            }
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}


