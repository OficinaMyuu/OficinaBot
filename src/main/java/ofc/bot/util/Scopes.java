package ofc.bot.util;

public final class Scopes {
    public static class Group {
        public static final String CREATE_GROUP   = "CREATE_GROUP";
        public static final String UPDATE_GROUP   = "UPDATE_GROUP";
        public static final String PAY_INVOICE    = "PAY_INVOICE";
        public static final String ADD_PERMISSION = "GROUP_PERMISSION_ADD";
        public static final String CREATE_CHANNEL = "CREATE_GROUP_CHANNEL";
        public static final String PIN_MESSAGE    = "TOGGLE_MESSAGE_PIN";
        public static final String ADD_BOT        = "GROUP_BOT_ADD";
        public static final String ADD_MEMBER     = "GROUP_MEMBER_ADD";
        public static final String REMOVE_MEMBER  = "GROUP_MEMBER_REMOVE";
    }

    public static class Punishments {
        public static final String VIEW_INFRACTIONS  = "VIEW_INFRACTIONS";
        public static final String DELETE_INFRACTION = "DELETE_INFRACTION";
    }

    public static class Reminders {
        public static final String VIEW_REMINDERS  = "VIEW_REMINDERS";
        public static final String DELETE_REMINDER = "DELETE_REMINDER";
    }

    public static class Economy {
        public static final String VIEW_LEADERBOARD  = "LEADERBOARD";
        public static final String VIEW_TRANSACTIONS = "VIEW_TRANSACTIONS";
    }

    public static class Bets {
        public static final String TICTACTOE_GAME   = "TICTACTOE_GAME";
        public static final String CREATE_TICTACTOE = "CREATE_TICTACTOE";
    }

    public static class Tickets {
        public static final String CREATE_TICKET = "CREATE_TICKET";
        public static final String DELETE_TICKET = "DELETE_TICKET";
        public static final String PAGINATE_TICKETS = "PAGINATE_TICKETS";
        public static final String DOWNLOAD_MESSAGES = "DOWNLOAD_MESSAGES";
    }

    public static class Misc {
        public static final String CHOOSABLE_ROLES            = "CHOOSABLE_ROLES";
        public static final String PAGINATE_BIRTHDAYS         = "VIEW_BIRTHDAYS";
        public static final String PAGINATE_LEVELS            = "VIEW_LEVELS";
        public static final String PAGINATE_NAME_UPDATE       = "USERNAME_UPDATE";
        public static final String PAGINATE_MARRIAGE_REQUESTS = "MARRIAGE_REQUESTS";
    }
}