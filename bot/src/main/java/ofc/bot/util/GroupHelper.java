package ofc.bot.util;

import ofc.bot.domain.entity.OficinaGroup;
import ofc.bot.domain.entity.enums.StoreItemType;
import ofc.bot.domain.sqlite.repository.GroupPerkRepository;

public class GroupHelper {
    private static GroupPerkRepository grpPerkRepo;

    private GroupHelper() {}

    public static boolean hasFreeSlots(OficinaGroup group) {
        return grpPerkRepo.countFree(group.getId(), StoreItemType.GROUP_SLOT) < OficinaGroup.INITIAL_SLOTS;
    }

    public static void setRepositories(GroupPerkRepository grpPerkRepo) {
        GroupHelper.grpPerkRepo = grpPerkRepo;
    }
}