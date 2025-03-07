package pt.tecnico.ulisboa.utils;

public class Logger {
    // DEBUGGING_ON is a value that can be set to control the level of debugging output
    //   0 - 50:    No debugging output
    //  51 - 150:   Only VLOG messages and high important messages
    // 151 - ...:   All messages including DEBUG messages

    public static int DEBUGGING_ON = 200;
    public static boolean LOGGING_ON = true;

    public static boolean IS_DEBUGGING() {
        return (DEBUGGING_ON > 150);
    }

    public static void LOG(String message) {
        if (LOGGING_ON) {
            if (DEBUGGING_ON > 50) VLOG(message);
            else QLOG(message);
        }
    }

    private static void VLOG(String message) {
        if (LOGGING_ON) {
            StackTraceElement element = getCallerStackTraceElement(true);
            String threadName = Thread.currentThread().getName();
            
            System.out.printf("[LOG] %s\n|_%s::%d::%s\n\n", 
                message, 
                element.getFileName(), 
                element.getLineNumber(),
                threadName);
        }
    }

    public static void QLOG(String message) {
        if (LOGGING_ON) {
            System.out.printf("[LOG] %s\n", message);
        }
    }

    public static void DEBUG(String message) {
        if (DEBUGGING_ON > 150) {
            StackTraceElement element = getCallerStackTraceElement(false);
            String threadName = Thread.currentThread().getName();
            
            System.out.printf("[DEBUG] %s\n|_%s::%d::%s\n\n", 
                message, 
                element.getFileName(), 
                element.getLineNumber(),
                threadName);
        }
    }

    public static void ERROR(String message) {
        StackTraceElement element = getCallerStackTraceElement(false);
        String threadName = Thread.currentThread().getName();

        System.out.printf("[ERROR] %s\n|_%s::%d::%s\n\n", 
            message, 
            element.getFileName(), 
            element.getLineNumber(),
            threadName);
        
        System.exit(-1);
    }

    public static void PRINT(String message) {
        if (DEBUGGING_ON > 150) {
            System.out.print(message);
        }
    }

    public static void PRINTLN(String message) {
        if (DEBUGGING_ON > 150) {
            System.out.println(message);
        }
    }

    public static void VPRINTLN(String message) {
        if (DEBUGGING_ON > 150) {
            StackTraceElement element = getCallerStackTraceElement(false);
            String threadName = Thread.currentThread().getName();
            
            System.out.printf("%s\n|_%s::%d::%s\n\n", 
                message, 
                element.getFileName(), 
                element.getLineNumber(),
                threadName);
        }
    }

    private static StackTraceElement getCallerStackTraceElement(boolean isTrueLog) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        return stackTrace[isTrueLog ? 4 : 3];
    }
}