package pt.tecnico.ulisboa.utils;

public class Logger {
    // DEBUGGING_ON is a value that can be set to control the level of debugging output
    //   0 - 50:    No debugging output
    //  51 - 150:   Only VLOG messages and high important messages
    // 151 - ...:   All messages including DEBUG messages

    public static int DEBUGGING_ON = 0;
    public static boolean LOGGING_ON = true;

    public static boolean IS_DEBUGGING() {
        return (DEBUGGING_ON > 50);
    }

    public static void LOG(String message) {
        if (LOGGING_ON) {
            if (DEBUGGING_ON > 50) {
                StackTraceElement element = getCallerStackTraceElement(true);
                String threadName = Thread.currentThread().getName();
                
                System.err.printf("[LOG] %s\n|_%s::%d::%s\n\n", 
                    message, 
                    element.getFileName(), 
                    element.getLineNumber(),
                    threadName);
            }
            else {
                System.out.printf("[LOG] %s\n", message);
            }
        }
    }

    public static void DEBUG(String message) {
        if (DEBUGGING_ON > 150) {
            StackTraceElement element = getCallerStackTraceElement(false);
            String threadName = Thread.currentThread().getName();
            
            System.err.printf("[DEBUG] %s\n|_%s::%d::%s\n\n", 
                message, 
                element.getFileName(), 
                element.getLineNumber(),
                threadName);
        }
    }

    public static void ERROR(String message) {
        StackTraceElement element = getCallerStackTraceElement(false);
        String threadName = Thread.currentThread().getName();

        System.err.printf("[ERROR] %s\n|_%s::%d::%s\n\n", 
            message, 
            element.getFileName(), 
            element.getLineNumber(),
            threadName);
        
        System.exit(-1);
    }

    public static void PRINT(String message) {
        if (DEBUGGING_ON > 50) {
            StackTraceElement element = getCallerStackTraceElement(false);
            String threadName = Thread.currentThread().getName();

            message = message + String.format("|_%s::%d::%s\n\n", 
                                                message, 
                                                element.getFileName(), 
                                                element.getLineNumber(),
                                                threadName
                                            );
            System.err.print(message);
        } else {
            System.out.print(message);
        }
    }

    public static void PRINTLN(String message) {
        if (DEBUGGING_ON > 50) {
            StackTraceElement element = getCallerStackTraceElement(false);
            String threadName = Thread.currentThread().getName();

            message = message + String.format("|_%s::%d::%s\n\n", 
                                                message, 
                                                element.getFileName(), 
                                                element.getLineNumber(),
                                                threadName
                                            );
            System.err.print(message);
        } else {
            System.out.println(message);
        }
    }

    private static StackTraceElement getCallerStackTraceElement(boolean isTrueLog) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        return stackTrace[isTrueLog ? 4 : 3];
    }
}