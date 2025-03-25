package pt.tecnico.ulisboa.utils.types;

import java.io.PrintStream;

public class Logger {
    // DEBUGGING_ON is a value that can be set to control the level of debugging
    // output
    // 0 - 50: No debugging output
    // 51 - 150: Only VLOG messages and high important messages
    // 151 - ...: All messages including DEBUG messages

    public static int DEBUGGING_ON = 151;
    public static boolean LOGGING_ON = true;
    public static int DEFAULT_STL = 3;

    public static boolean IS_DEBUGGING() {
        return (DEBUGGING_ON > 50);
    }

    public static void LOG(String message) {
        PrintStream stream;
        if (DEBUGGING_ON > 50) stream = System.err;
        else stream = System.out;

        LOG(stream, message, DEFAULT_STL + 1);
    }

    public static void LOG(PrintStream stream, String message) {
        LOG(stream, message, DEFAULT_STL + 1);
    }

    public static void LOG(PrintStream stream, String message, int ste) {
        if (LOGGING_ON) {
            if (DEBUGGING_ON > 50) {
                StackTraceElement element = getCallerStackTraceElement(ste);
                String threadName = Thread.currentThread().getName();

                stream.printf("[LOG] %s\n|_%s::%d::%s\n\n",
                        message,
                        element.getFileName(),
                        element.getLineNumber(),
                        threadName);
            } else {
                stream.printf("[LOG] %s\n", message);
            }
            stream.flush();
        }
    }

    public static void DEBUG(String message) {
        DEBUG(System.err, message, DEFAULT_STL + 1);
    }

    public static void DEBUG(PrintStream stream, String message) {
        DEBUG(stream, message, DEFAULT_STL + 1);
    }

    public static void DEBUG(PrintStream stream, String message, int ste) {
        if (DEBUGGING_ON > 150) {
            StackTraceElement element = getCallerStackTraceElement(ste);
            String threadName = Thread.currentThread().getName();

            stream.printf("[DEBUG] %s\n|_%s::%d::%s\n\n",
                    message,
                    element.getFileName(),
                    element.getLineNumber(),
                    threadName);
        }
    }

    public static void ERROR(String message) {
        ERROR(System.err, message, null, DEFAULT_STL + 1);
    }

    public static void ERROR(String message, Exception e) {
        ERROR(System.err, message, e, DEFAULT_STL + 1);
    }

    public static void ERROR(PrintStream stream, String message) {
        ERROR(stream, message, null, DEFAULT_STL + 1);
    }

    public static void ERROR(PrintStream stream, String message, Exception e) {
        ERROR(stream, message, e, DEFAULT_STL + 1);
    }

    public static void ERROR(PrintStream stream, String message, Exception e, int ste) {
        StackTraceElement element = getCallerStackTraceElement(ste);
        String threadName = Thread.currentThread().getName();

        stream.printf("[ERROR] %s\n|_%s::%d::%s\n\n",
                message,
                element.getFileName(),
                element.getLineNumber(),
                threadName);

        if (e != null) {
            e.printStackTrace();
            stream.println();
        }

        System.exit(-1);
    }

    public static void PRINT(String message) {
        PrintStream stream;
        if (DEBUGGING_ON > 50) stream = System.err;
        else stream = System.out;

        PRINT(stream, message, 3);
    }

    public static void PRINT(PrintStream stream, String message) {
        PRINT(stream, message, 3);
    }

    public static void PRINT(PrintStream stream, String message, int ste) {
        if (DEBUGGING_ON > 50) {
            StackTraceElement element = getCallerStackTraceElement(ste);
            String threadName = Thread.currentThread().getName();

            message = message + String.format("|_%s::%d::%s\n\n",
                    message,
                    element.getFileName(),
                    element.getLineNumber(),
                    threadName);
            stream.print(message);
        } else {
            stream.print(message);
        }
    }

    public static void PRINTLN(String message) {
        PrintStream stream;
        if (DEBUGGING_ON > 50) stream = System.err;
        else stream = System.out;

        PRINTLN(stream, message, DEFAULT_STL + 1);
    }

    public static void PRINTLN(PrintStream stream, String message) {
        PRINTLN(stream, message, DEFAULT_STL + 1);
    }

    public static void PRINTLN(PrintStream stream, String message, int ste) {
        if (DEBUGGING_ON > 50) {
            StackTraceElement element = getCallerStackTraceElement(ste);
            String threadName = Thread.currentThread().getName();

            message = message + String.format("|_%s::%d::%s\n\n",
                    message,
                    element.getFileName(),
                    element.getLineNumber(),
                    threadName);
            stream.print(message);
        } else {
            stream.println(message);
        }
    }

    private static StackTraceElement getCallerStackTraceElement(int level) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        return stackTrace[level];
    }
}