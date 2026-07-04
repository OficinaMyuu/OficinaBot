package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.MessageTranscription;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.impl.SQLDataType;

public class MessagesTranscriptionsTable extends InitializableTable<MessageTranscription> {
    public static final MessagesTranscriptionsTable MESSAGES_TRANSCRIPTIONS = new MessagesTranscriptionsTable();

    public final Field<Integer> ID             = newField("id",              SQLDataType.INTEGER.identity(true));
    public final Field<Long> MESSAGE_ID        = newField("message_id",      SQLDataType.BIGINT.notNull());
    public final Field<Long> CHANNEL_ID        = newField("channel_id",      SQLDataType.BIGINT.notNull());
    public final Field<Long> REQUESTER_ID      = newField("requester_id",    SQLDataType.BIGINT.notNull());
    public final Field<Double> AUDIO_LENGTH    = newField("audio_length",    SQLDataType.DOUBLE.notNull());
    public final Field<String> TRANSCRIPTION   = newField("transcription",   SQLDataType.VARCHAR(255).notNull());
    public final Field<String> FILE_EXTENSION  = newField("file_extension",  SQLDataType.VARCHAR(255).notNull());
    public final Field<Boolean> IS_HARMFUL     = newField("is_harmful",      SQLDataType.BOOLEAN);
    public final Field<Double> SEXUAL_SCORE    = newField("sexual_score",    SQLDataType.DOUBLE);
    public final Field<Double> HATE_SCORE      = newField("hate_score",      SQLDataType.DOUBLE);
    public final Field<Double> ILLICIT_SCORE   = newField("illicit_score",   SQLDataType.DOUBLE);
    public final Field<Double> SELF_HARM_SCORE = newField("self_harm_score", SQLDataType.DOUBLE);
    public final Field<Double> VIOLENCE_SCORE  = newField("violence_score",  SQLDataType.DOUBLE);
    public final Field<Long> CREATED_AT        = newField("created_at",      SQLDataType.BIGINT.notNull());

    public MessagesTranscriptionsTable() {
        super("message_transcriptions");
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields())
                .unique(REQUESTER_ID, CHANNEL_ID, MESSAGE_ID)
                .unique(CHANNEL_ID, MESSAGE_ID)
                .check(SEXUAL_SCORE.ge(0d).or(SEXUAL_SCORE.isNull()))
                .check(HATE_SCORE.ge(0d).or(HATE_SCORE.isNull()))
                .check(ILLICIT_SCORE.ge(0d).or(ILLICIT_SCORE.isNull()))
                .check(SELF_HARM_SCORE.ge(0d).or(SELF_HARM_SCORE.isNull()))
                .check(VIOLENCE_SCORE.ge(0d).or(VIOLENCE_SCORE.isNull()));
    }

    @NotNull
    @Override
    public Class<MessageTranscription> getRecordType() {
        return MessageTranscription.class;
    }
}