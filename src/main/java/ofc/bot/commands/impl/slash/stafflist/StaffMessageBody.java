package ofc.bot.commands.impl.slash.stafflist;

record StaffMessageBody(
        String title,
        String role,
        String message,
        String footer
) {}