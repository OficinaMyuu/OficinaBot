package ofc.bot.handlers;

import ofc.bot.domain.sqlite.DB;
import org.jooq.*;
import org.jooq.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class ConsoleQueryHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleQueryHandler.class);
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final JSONFormat DEFAULT_JSON_FORMAT = new JSONFormat().recordFormat(JSONFormat.RecordFormat.OBJECT);
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final DSLContext CTX = DB.getContext();

    public static void init() {
        try {
            EXECUTOR.execute(ConsoleQueryHandler::run);
            LOGGER.info("Successfully initialized ConsoleQueryHandler thread");
        } catch (RejectedExecutionException e) {
            LOGGER.error("Executor rejected the ConsoleQueryHandler operation", e);
        }
    }

    private static void run() {
        while (true) {
            String sql = SCANNER.nextLine();

            if (sql == null || sql.isBlank()) {
                LOGGER.warn("SQL queries may not be empty");
                continue;
            }
            executeAndPrettyPrint(sql);
        }
    }

    private static void executeAndPrettyPrint(String sql) {
        try {
            Result<Record> res = CTX.fetch(sql);
            int size = res.size();

            if (size == 0) {
                LOGGER.warn("No results found");
                return;
            }

            if (size == 1) {
                Record rec = res.getFirst();
                LOGGER.info("Result:\n{}", rec.formatJSON(DEFAULT_JSON_FORMAT));
            } else {
                LOGGER.info("Results:\n{}", res.formatJSON(DEFAULT_JSON_FORMAT));
            }
        } catch (Exception e) {
            LOGGER.error("Could not execute SQL command because: {}", e.getMessage());
        }
    }
}