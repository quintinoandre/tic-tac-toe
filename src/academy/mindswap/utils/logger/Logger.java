package academy.mindswap.utils.logger;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Class that registers the app logs.
 */
public final class Logger {
    private static Logger logger;

    private Logger() {
    }

    /**
     * Ensures that only one instance of the class is created.
     *
     * @return
     */
    public static Logger singleton() {
        if (logger == null) {
            logger = new Logger();
        }

        return logger;
    }

    /**
     * Intermediate step to call the right logger method.
     */
    public static void log(LoggerType loggerType, String message, boolean printOnTerminal) {
        singleton().writeLog(loggerType, message, printOnTerminal);
    }

    /**
     * Method that creates and updates the logger files.
     *
     * @param loggerType      is the type of log that will be created and updated.
     * @param message         is the String message that will be written inside the file.
     * @param printOnTerminal boolean that decides if the message gets printed on the terminal.
     */
    private void writeLog(LoggerType loggerType, String message, boolean printOnTerminal) {
        FileWriter writer = null;

        try {
            writer = new FileWriter("resources/logs/".concat(loggerType.getDescription()).concat("-logs.log"), true);

            writer.write(LocalDateTime.now() + " - ".concat(message.concat("\n")));

            if (printOnTerminal) {
                System.out.println(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}