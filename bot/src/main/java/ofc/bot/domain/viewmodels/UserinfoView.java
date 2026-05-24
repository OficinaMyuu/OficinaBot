package ofc.bot.domain.viewmodels;

import ofc.bot.domain.entity.CustomUserinfo;
import ofc.bot.domain.entity.OficinaGroup;

import java.util.List;

public record UserinfoView(
        CustomUserinfo mods,
        OficinaGroup group,
        List<MarriageView> marriages,
        int marriageCount,
        long userId,
        long balance
) {}