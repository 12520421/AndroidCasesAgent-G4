package com.xzfg.app;

import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DebugLog {

    final static int LOGGER_ENTRY_MAX_LEN = 3000 /* (4 * 1024) */;

    static String className;
    static String methodName;
    static int lineNumber;
    static int count = 0;
    static String logs = "";

    final static boolean appendLogFile = false;

    private DebugLog() {
        /* Protect from instantiations */
    }

    public static boolean isDebuggable() {
        return true;
    }

    public static String getLogs() {
        return logs.toString();
    }

    private static synchronized void appenLog(String log) {
        if (count >= 400) {
            logs = "";
            count = 0;
        }
        count++;
        logs = log + logs;
        if (appendLogFile)
            appendLogFile(log);
    }

    public static void onLowMemory() {
        logs = "";
        count = 0;
    }

    private static String createLog(String log) {

        StringBuffer buffer = new StringBuffer();
        buffer.append("[");
        buffer.append(methodName);
        buffer.append(":");
        buffer.append(lineNumber);
        buffer.append("]");
        buffer.append(log);

        return buffer.toString();
    }

    private static void getMethodNames(StackTraceElement[] sElements) {
        className = sElements[1].getFileName();
        methodName = sElements[1].getMethodName();
        lineNumber = sElements[1].getLineNumber();
    }

    public static void e(String message) {
        if (!isDebuggable())
            return;

        // Throwable instance must be created before any methods
        getMethodNames(new Throwable().getStackTrace());
        String log = createLog(message);
        appenLog(log + "\n");

        // show full log
        int left = -1, right = 0;
        int lenght = log.length();
        while (lenght != 0) {
            left = right;
            if (lenght >= LOGGER_ENTRY_MAX_LEN) {
                right += LOGGER_ENTRY_MAX_LEN;
                lenght -= LOGGER_ENTRY_MAX_LEN;
            } else {
                right = log.length();
                lenght = 0;
            }
            try {
                Log.e(className, log.substring(left, right));
            } catch (StringIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }

    public static void i(String message) {
        if (!isDebuggable())
            return;

        getMethodNames(new Throwable().getStackTrace());
        String log = createLog(message);
        appenLog(log + "\n");

        // show full log
        int left = -1, right = 0;
        int lenght = log.length();
        while (lenght != 0) {
            left = right;
            if (lenght >= LOGGER_ENTRY_MAX_LEN) {
                right += LOGGER_ENTRY_MAX_LEN;
                lenght -= LOGGER_ENTRY_MAX_LEN;
            } else {
                right = log.length();
                lenght = 0;
            }
            try {
                Log.i(className, log.substring(left, right));
            } catch (StringIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }

    public static void d(String message) {
        if (!isDebuggable())
            return;

        getMethodNames(new Throwable().getStackTrace());
        String log = createLog(message);
        appenLog(log + "\n");

        // show full log
        int left = -1, right = 0;
        int lenght = log.length();
        while (lenght != 0) {
            left = right;
            if (lenght >= LOGGER_ENTRY_MAX_LEN) {
                right += LOGGER_ENTRY_MAX_LEN;
                lenght -= LOGGER_ENTRY_MAX_LEN;
            } else {
                right = log.length();
                lenght = 0;
            }
            try {
                Log.d(className, log.substring(left, right));
            } catch (StringIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }

    public static void v(String message) {
        if (!isDebuggable())
            return;

        getMethodNames(new Throwable().getStackTrace());
        String log = createLog(message);

        // show full log
        int left = -1, right = 0;
        int lenght = log.length();
        while (lenght != 0) {
            left = right;
            if (lenght >= LOGGER_ENTRY_MAX_LEN) {
                right += LOGGER_ENTRY_MAX_LEN;
                lenght -= LOGGER_ENTRY_MAX_LEN;
            } else {
                right = log.length();
                lenght = 0;
            }
            try {
                Log.v(className, log.substring(left, right));
            } catch (StringIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }

    public static void w(String message) {
        if (!isDebuggable())
            return;

        getMethodNames(new Throwable().getStackTrace());
        String log = createLog(message);

        // show full log
        int left = -1, right = 0;
        int lenght = log.length();
        while (lenght != 0) {
            left = right;
            if (lenght >= LOGGER_ENTRY_MAX_LEN) {
                right += LOGGER_ENTRY_MAX_LEN;
                lenght -= LOGGER_ENTRY_MAX_LEN;
            } else {
                right = log.length();
                lenght = 0;
            }
            try {
                Log.w(className, log.substring(left, right));
            } catch (StringIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }

    public static void wtf(String message) {
        if (!isDebuggable())
            return;

        getMethodNames(new Throwable().getStackTrace());
        String log = createLog(message);

        // show full log
        int left = -1, right = 0;
        int lenght = log.length();
        while (lenght != 0) {
            left = right;
            if (lenght >= LOGGER_ENTRY_MAX_LEN) {
                right += LOGGER_ENTRY_MAX_LEN;
                lenght -= LOGGER_ENTRY_MAX_LEN;
            } else {
                right = log.length();
                lenght = 0;
            }
            try {
                Log.wtf(className, log.substring(left, right));
            } catch (StringIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }

    public static void debugJson(Object object) {
        debugJson("debugJson:", object);
    }

    public static void debugJson(String title, Object object) {
        if (!isDebuggable())
            return;
        try {
            e(title + new Gson().toJson(object));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void appendLogFile(Object text) {
        File logFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Home.file");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(String.valueOf(text));
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
