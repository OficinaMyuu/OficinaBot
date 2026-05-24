package ofc.bot.commands.impl.slash.stafflist;

import java.util.List;

record InputData(
        String banner,
        String bannerMessageId,
        List<StaffMessageBody> staffs
) {}