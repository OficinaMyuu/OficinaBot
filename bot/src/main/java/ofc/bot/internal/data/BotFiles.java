package ofc.bot.internal.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class BotFiles {
    private static final Logger LOGGER = LoggerFactory.getLogger(BotFiles.class);
    public static final File DIR_CONTENT = new File("content");
    public static final File DIR_ASSETS = new File("assets");
    public static final File DATABASE = new File("database.db");
    private static final List<File> FILES = List.of(
            new File(DIR_CONTENT, "staffconfig.json")
    );

    private BotFiles() {}

    public static void loadFiles() throws IOException {
        for (File file : FILES) {
            Path path = Paths.get(file.getAbsolutePath());
            Files.createDirectories(path.getParent());

            if (file.createNewFile())
                LOGGER.warn("File {} was not found! Creating a new one at {}.", file.getName(), path.toAbsolutePath());
            else
                LOGGER.info("File {} was successfully found at {}!", file.getName(), path.toAbsolutePath());
        }
    }
}