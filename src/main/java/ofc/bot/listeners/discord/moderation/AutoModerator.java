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
import ofc.bot.domain.sqlite.repository.*;
import ofc.bot.handlers.cache.PolicyService;
import ofc.bot.handlers.moderation.PunishmentData;
import ofc.bot.handlers.moderation.PunishmentManager;
import ofc.bot.handlers.moderation.Reason;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ofc.bot.domain.entity.enums.PolicyType.*;

@DiscordEventHandler
public class AutoModerator extends ListenerAdapter {
    private static final ErrorHandler DEFAULT_ERROR_HANDLER = new ErrorHandler().ignore(ErrorResponse.CANNOT_SEND_TO_USER);
    private static final LocalTime NIGHT_LIMIT = LocalTime.of(6, 0);

    /* REGEX patterns */
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");
    private static final Pattern REPEATED_WORD_PATTERN = Pattern.compile("(\\S+)(?:\\s+\\1){5,}");
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
    private final SupportTicketRepository ticketRepo;

    public AutoModerator(
            BlockedWordRepository blckWordsRepo, MemberPunishmentRepository pnshRepo,
            AutomodActionRepository modActRepo, SupportTicketRepository ticketRepo
    ) {
        this.blockedWordsCache = loadBlockedWords(blckWordsRepo);
        this.punishmentManager = new PunishmentManager(pnshRepo, modActRepo);
        this.ticketRepo = ticketRepo;
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent e) {
        if (e.getAuthor().isBot() || !e.isFromGuild()) return;
        runChecks(e.getMessage());
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        if (e.getAuthor().isBot() || e.isWebhookMessage() || !e.isFromGuild()) return;

        /*
         * Prevents "ghost moderation" of ancient messages.
         * Discord frequently fires MessageUpdateEvents for old messages when
         * background metadata changes (like when an old link's embed refreshes).
         * If we don't ignore old messages, the bot will evaluate messages from years ago
         * against today's automod rules, resulting in confusing warnings/deletions for
         * things users said in the past.
         */
        if (isOld(e.getMessage())) return;

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

        // Ticket channels are not affected by moderation
        if (ticketRepo.isTicketChannel(chan)) return;

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
        String[] words = getSanitizedWords(content);
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

    private String[] getSanitizedWords(String msg) {
        return msg.toLowerCase()
                .replaceAll("[*_~`#/@<>\\[\\]{}?!%$]", "")
                .split("\\s+");
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
        Matcher matcher = URL_PATTERN.matcher(content.toLowerCase());

        while (matcher.find()) {
            String urlString = matcher.group();

            try {
                URI uri = new URI(urlString);
                String domain = uri.getHost();

                if (domain == null || !isAllowedDomain(domain)) return true;
            } catch (URISyntaxException e) {
                return true;
            }
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
        return ids.contains(chanId) || ids.contains(member.getIdLong()) || member.getRoles()
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

    private boolean isOld(Message msg) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime messageTime = msg.getTimeCreated();

        long distanceMins = ChronoUnit.MINUTES.between(messageTime, now);

        return distanceMins > 10;
    }

    static {
        UNICODE_EMOJI_PATTERN = Pattern.compile(
                "[" +
                        "\\x{1F300}-\\x{1F5FF}" + // Misc Symbols and Pictographs
                        "\\x{1F600}-\\x{1F64F}" + // Emoticons
                        "\\x{1F680}-\\x{1F6FF}" + // Transport and Map
                        "\\x{1F700}-\\x{1F77F}" + // Alchemical Symbols
                        "\\x{1F780}-\\x{1F7FF}" + // Geometric Shapes Extended
                        "\\x{1F800}-\\x{1F8FF}" + // Supplemental Arrows-C
                        "\\x{1F900}-\\x{1F9FF}" + // Supplemental Symbols and Pictographs
                        "\\x{1FA00}-\\x{1FA6F}" + // Chess Symbols
                        "\\x{1FA70}-\\x{1FAFF}" + // Symbols and Pictographs Extended-A
                        "\\x{2600}-\\x{26FF}" +   // Misc symbols
                        "\\x{2700}-\\x{27BF}" +   // Dingbats
                        "\\x{2300}-\\x{23FF}" +   // Misc Technical
                        "\\x{2B50}\\x{2B55}"    + // Star and O
                        "\\x{2934}-\\x{2935}"   + // Arrow turning
                        "\\x{2B05}-\\x{2B07}"   + // Directional arrows
                        "\\x{2B1B}-\\x{2B1C}"   + // Black/White large squares
                        "\\x{00A9}\\x{00AE}"    + // Copyright / Registered
                        "\\x{2122}"             + // Trademark
                        "]"
        );
    }
}