package ofc.bot.handlers;

import com.google.gson.FormattingStyle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ofc.bot.domain.sqlite.DB;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class ConsoleQueryHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleQueryHandler.class);
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final DSLContext CTX = DB.getContext();
    private static final Gson PRETTY_GSON;
    private static final Gson GSON;

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
        // Gladly we don't have to remove it from the string when executing
        // the query, as the database will ignore it, being just a comment lol
        boolean isPretty = sql.endsWith("--pretty");

        try {
            Result<Record> res = CTX.fetch(sql);
            int size = res.size();

            if (size == 0) {
                LOGGER.warn("No results found");
                return;
            }

            if (size == 1) {
                Record rec = res.getFirst();
                LOGGER.info("Result:\n{}", toJson(rec, isPretty));
            } else {
                LOGGER.info("Results:\n{}", toJson(res, isPretty));
            }
        } catch (Exception e) {
            LOGGER.error("Could not execute SQL command because: {}", e.getMessage());
        }
    }

    private static String toJson(Record rec, boolean pretty) {
        Map<String, Object> map = rec.intoMap();
        return pretty ? PRETTY_GSON.toJson(map) : GSON.toJson(map);
    }

    private static String toJson(Result<Record> recs, boolean pretty) {
        List<Map<String, Object>> maps = recs.intoMaps();
        return pretty ? PRETTY_GSON.toJson(maps) : GSON.toJson(maps);
    }

    static {
        GsonBuilder gsonBuilder = new GsonBuilder().serializeNulls().disableHtmlEscaping();
        PRETTY_GSON = gsonBuilder.setFormattingStyle(FormattingStyle.PRETTY).create();
        GSON = gsonBuilder.setFormattingStyle(FormattingStyle.COMPACT).create();
    }
}