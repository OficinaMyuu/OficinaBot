package ofc.bot.listeners.discord.moderation;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.messages.MessageSnapshot;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import ofc.bot.domain.entity.BlockedWord;
import ofc.bot.domain.entity.enums.PolicyType;
import ofc.bot.domain.sqlite.repository.AutomodActionRepository;
import ofc.bot.domain.sqlite.repository.BlockedWordRepository;
import ofc.bot.domain.sqlite.repository.MemberPunishmentRepository;
import ofc.bot.handlers.cache.PolicyService;
import ofc.bot.handlers.moderation.PunishmentData;
import ofc.bot.handlers.moderation.PunishmentManager;
import ofc.bot.handlers.moderation.Reason;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ofc.bot.domain.entity.enums.PolicyType.*;

@DiscordEventHandler
public class AutoModerator extends ListenerAdapter {
    private static final ErrorHandler DEFAULT_ERROR_HANDLER = new ErrorHandler().ignore(ErrorResponse.CANNOT_SEND_TO_USER);
    private static final LocalTime NIGHT_LIMIT = LocalTime.of(6, 0);

    /* REGEX patterns */
    private static final Pattern URL_PATTERN = Pattern.compile("https?://([\\w.-]+(?:\\.[\\w.-]+)+)[/\\w\\-.?=&%]*");
    private static final Pattern REPEATED_WORD_PATTERN = Pattern.compile("\\b(\\w+)\\b(?:\\s+\\1\\b){5,}");
    private static final Pattern REPEATED_CHAR_PATTERN = Pattern.compile("(.)\\1{59,}");
    private static final Pattern MENTION_PATTERN = Pattern.compile("<@!?(\\d+)>");
    private static final Pattern DISCORD_EMOJI_PATTERN = Pattern.compile("<a?:\\w+:[0-9]+>|:[a-zA-Z0-9_]+:");
    private static final Pattern UNICODE_EMOJI_PATTERN;

    /* Default Reasons */
    private static final String REASON_BLOCKED_WORDS = "Palavras Proibidas";
    private static final String REASON_MASS_EMOJI    = "Emoji em Massa";
    private static final String REASON_MASS_MENTION  = "Menção em Massa";
    private static final String REASON_SEND_LINKS    = "Enviou Links";
    private static final String REASON_REPEATED_TEXT = "Texto Repetido";
    private static final String REASON_SEND_INVITE   = "Enviou Convite";

    /* Limitations */
    public static final int MAX_EMOJIS = 10;
    public static final int MAX_MENTIONS = 5;

    /* Cache, Managers and some nice non-static stuff */
    private final PolicyService policyCache = PolicyService.getService();
    private final Map<Long, List<BlockedWord>> blockedWordsCache;
    private final PunishmentManager punishmentManager;

    public AutoModerator(
            BlockedWordRepository blckWordsRepo, MemberPunishmentRepository pnshRepo,
            AutomodActionRepository modActRepo
    ) {
        this.blockedWordsCache = loadBlockedWords(blckWordsRepo);
        this.punishmentManager = new PunishmentManager(pnshRepo, modActRepo);
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent e) {
        if (e.getAuthor().isBot() || !e.isFromGuild()) return;
        runChecks(e.getMessage());
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        if (e.getAuthor().isBot() || e.isWebhookMessage() || !e.isFromGuild()) return;
        runChecks(e.getMessage());
    }

    @SuppressWarnings("DataFlowIssue")
    private void runChecks(Message msg) {
        Reason warnReasons = new Reason();
        Reason delReasons = new Reason();

        MessageChannel chan = msg.getChannel();
        List<MessageSnapshot> snapshots = msg.getMessageSnapshots();
        Member member = msg.getMember();
        String content = msg.getContentRaw();
        Guild guild = msg.getGuild();
        Member self = guild.getSelfMember();
        long chanId = chan.getIdLong();

        // Immune to every kind of moderation
        if (member.hasPermission(Permission.MANAGE_SERVER)) return;

        // Checking moderations for the member's message
        validateContent(delReasons, warnReasons, content, member, chanId, hasInvites(msg));

        // Checking moderations for the snapshots messages of the member's forwarded message
        for (MessageSnapshot snapshot : snapshots) {
            String snapshotContent = snapshot.getContentRaw();
            validateContent(delReasons, warnReasons, snapshotContent, member, chanId, hasInvites(snapshot));
        }

        // Applying moderation actions
        if (!delReasons.isEmpty()) {
            advertMember(member.getUser(), delReasons);
            Bot.delete(msg);
        }

        if (!warnReasons.isEmpty()) {
            PunishmentData warnData = new PunishmentData(chan, member, self, warnReasons);
            MessageEmbed embed = punishmentManager.createPunishment(warnData);
            chan.sendMessageEmbeds(embed).queue();
        }
    }

    private void validateContent(
            Reason delReasons, Reason warnReasons, String content,
            Member member, long chanId, boolean hasInvites
    ) {
        long guildId = member.getGuild().getIdLong();

        // Checking for blocked words
        List<BlockedWord> guildBlocked = blockedWordsCache.getOrDefault(guildId, List.of());
        if (!isExcluded(BYPASS_WORD_BLOCKER, member, chanId) && hasBlockedWords(content, guildBlocked)) {
            delReasons.add(REASON_BLOCKED_WORDS);
        }

        // Checking for excessive emojis
        if (!isExcluded(BYPASS_MASS_EMOJI_BLOCKER, member, chanId) && hasMassEmoji(content)) {
            delReasons.add(REASON_MASS_EMOJI);
            warnReasons.add(REASON_MASS_EMOJI);
        }

        // Checking for excessive mentions
        if (!isExcluded(BYPASS_MASS_MENTION_BLOCKER, member, chanId) && hasMassMentions(content)) {
            delReasons.add(REASON_MASS_MENTION);
        }

        // Checking for links
        if (!isExcluded(BYPASS_LINKS_BLOCKER, member, chanId) && hasLinks(content)) {
            delReasons.add(REASON_SEND_LINKS);
        }

        // Checking for repeated text
        if (!isExcluded(BYPASS_REPEATS_BLOCKER, member, chanId) && hasRepeatedContent(content)) {
            delReasons.add(REASON_REPEATED_TEXT);
        }

        // Checking for invites
        if (!isExcluded(BYPASS_INVITES_BLOCKER, member, chanId) && hasInvites) {
            delReasons.add(REASON_SEND_INVITE);
            warnReasons.add(REASON_SEND_INVITE);
        }
    }

    private boolean hasBlockedWords(String content, List<BlockedWord> blockedWords) {
        String[] words = content.toLowerCase().split(" ");
        LocalTime now = LocalTime.now();
        boolean isNight = now.isAfter(LocalTime.MIDNIGHT) && now.isBefore(NIGHT_LIMIT);

        for (BlockedWord blck : blockedWords) {
            if (!blck.isSevere() && isNight) continue;

            for (String word : words) {
                if (blck.isMatchExact() && word.equals(blck.getWord())) return true;
                if (!blck.isMatchExact() && word.contains(blck.getWord())) return true;
            }
        }
        return false;
    }

    private boolean hasMassEmoji(String content) {
        int emojis = countMatches(UNICODE_EMOJI_PATTERN, content);
        if (emojis > MAX_EMOJIS) return true;

        emojis += countMatches(DISCORD_EMOJI_PATTERN, content);
        return emojis > MAX_EMOJIS;
    }

    private boolean hasMassMentions(String content) {
        return countMatches(MENTION_PATTERN, content) > MAX_MENTIONS;
    }

    private boolean hasLinks(String content) {
        String normalized = content.toLowerCase();
        Matcher matcher = URL_PATTERN.matcher(normalized);
        while (matcher.find()) {
            String domain = matcher.group(1);
            if (!isAllowedDomain(domain)) return true;
        }
        return false;
    }

    private boolean isAllowedDomain(String domain) {
        return policyCache.isDomainAllowed(domain);
    }

    private boolean hasRepeatedContent(String content) {
        String normalized = content.toLowerCase();

        return REPEATED_CHAR_PATTERN.matcher(normalized).find()
                || REPEATED_WORD_PATTERN.matcher(normalized).find();
    }

    private boolean hasInvites(Message msg) {
        return !msg.getInvites().isEmpty();
    }

    private boolean hasInvites(MessageSnapshot msg) {
        return !msg.getInvites().isEmpty();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isExcluded(PolicyType type, Member member, long chanId) {
        Set<Long> ids = policyCache.get(type, Long::parseLong);
        return ids.contains(chanId) || member.getRoles()
                .stream()
                .anyMatch(r -> ids.contains(r.getIdLong()));
    }

    private int countMatches(Pattern pattern, String content) {
        Matcher matcher = pattern.matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private void advertMember(User user, Reason reasons) {
        String message = getAdvertMessage(user, reasons);
        user.openPrivateChannel()
                .flatMap(chan -> chan.sendMessage(message))
                .queue(null, DEFAULT_ERROR_HANDLER);
    }

    private String getAdvertMessage(User user, Reason reasons) {
        // Pretty printing ^^
        String ruleCount = reasons.size() == 1 ? "1 regra" : String.format("%d regras", reasons.size());

        return String.format("%s, por favor, não envie este tipo de mensagem aqui! Ela viola %s: %s.",
                user.getAsMention(), ruleCount, reasons);
    }

    private Map<Long, List<BlockedWord>> loadBlockedWords(BlockedWordRepository blckWordsRepo) {
        Map<Long, List<BlockedWord>> blockedMap = new HashMap<>();
        List<BlockedWord> words = blckWordsRepo.findAll();

        for (BlockedWord word : words) {
            long guildId = word.getGuildId();
            List<BlockedWord> blockedList = blockedMap.getOrDefault(guildId, new ArrayList<>());
            blockedList.add(word);
            blockedMap.put(guildId, blockedList);
        }
        return blockedMap;
    }

    // Thank you ChatGPT for providing such useful REGEXes ^^
    static {
        //noinspection RegExpRedundantNestedCharacterClass,UnnecessaryUnicodeEscape
        UNICODE_EMOJI_PATTERN = Pattern.compile(
                "[" +
                        "\u203C-\u3299" +             // Various symbols
                        "\uD83C\uDC04" +              // Some specific emojis
                        "\uD83C\uDCCF" +
                        "\uD83C\uDDE6-\uD83C\uDDFF" + // Regional indicator symbols
                        "\uD83C\uDE01-\uD83C\uDE4F" + // Enclosed characters, etc.
                        "\uD83C\uDE50-\uD83C\uDEFF" +
                        "\uD83D\uDC00-\uD83D\uDE4F" + // Emoticons
                        "\uD83D\uDE80-\uD83D\uDEF6" + // Transport & map symbols
                        "\uD83E\uDD00-\uD83E\uDDFF" + // Supplemental Symbols and Pictographs
                        "\uD83D\uDD00-\uD83D\uDDFF" + // Additional ranges sometimes needed
                        "\uD83F\uDC00-\uD83F\uDFFF" + // Hypothetical future range (if needed)
                        "\uD83C\uDF00-\uD83C\uDFFF" + // Some extra pictographs
                        "\uD83F\uDF00-\uD83F\uDFFF" + // Some extra symbols (if needed)
                        "\uD83D\uDF00-\uD83D\uDFFF" + // Just in case
                        "\uD83E\uDF00-\uD83E\uDFFF" + // Another extra block
                        "\uD83D[\uDFE0-\uDFEF]" +     // Range for colored circle emojis (U+1F7E0 to U+1F7FF)
                "]+"
        );
    }
}