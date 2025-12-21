package ofc.bot.jobs;

import ofc.bot.commands.impl.slash.relationships.MarryCommand;
import ofc.bot.domain.entity.Marriage;
import ofc.bot.domain.entity.UserEconomy;
import ofc.bot.domain.entity.enums.PolicyType;
import ofc.bot.domain.sqlite.repository.*;
import ofc.bot.util.content.annotations.jobs.CronJob;
import org.quartz.*;

import java.util.*;
import java.util.stream.Collectors;

@CronJob(expression = "0 0 0 ? * * *") // Every day at midnight
public class DailyMarriageCharger implements Job {
    private static final int DAILY_COST = MarryCommand.DAILY_COST;
    private final MarriageRepository marrRepo;
    private final UserEconomyRepository ecoRepo;
    private final EntityPolicyRepository policyRepo;

    public DailyMarriageCharger() {
        this.marrRepo = Repositories.getMarriageRepository();
        this.ecoRepo = Repositories.getUserEconomyRepository();
        this.policyRepo = Repositories.getEntityPolicyRepository();
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Set<Long> exemptedUsers = policyRepo.findSetByType(PolicyType.EXEMPT_MARRIAGE_FEE, Long::parseLong);
        Map<Long, List<Marriage>> marriages = fetchMappedMarriages(exemptedUsers);
        Map<Long, UserEconomy> ecos = fetchMappedEconomy(exemptedUsers);

        for (long userId : marriages.keySet()) {
            // This list is already ordered in ascending order (based on marriage creation)
            List<Marriage> rels = marriages.get(userId);
            UserEconomy balance = ecos.getOrDefault(userId, UserEconomy.fromUserId(userId));
            int bank = balance.getBank();
            int cost = DAILY_COST * rels.size();

            // User cannot afford all their relationships, so we divorce the additionals
            if (cost > bank) {
                // Integer division will be truncated, no need for Math.floorDiv()
                int affordable = bank / DAILY_COST;
                divorce(marriages, userId, affordable);
            }


        }
    }

    /**
     * Divorces the amount of relationships from the provided user.
     *
     * @param userId The ID of the user to be divorced.
     * @param count The amount of relationships to be deleted/divorced.
     */
    private void divorce(Map<Long, List<Marriage>> marriages, long userId, int count) {

    }

    private Map<Long, List<Marriage>> fetchMappedMarriages(Set<Long> exemptedUsers) {
        List<Marriage> marriages = marrRepo.findAllExcept(exemptedUsers);
        Map<Long, List<Marriage>> map = new HashMap<>(marriages.size());

        for (Marriage marr : marriages) {
            long reqId = marr.getRequesterId();
            long tarId = marr.getTargetId();

            List<Marriage> reqList = map.getOrDefault(reqId, new ArrayList<>());
            List<Marriage> tarList = map.getOrDefault(tarId, new ArrayList<>());

            reqList.add(marr);
            tarList.add(marr);

            map.put(reqId, tarList);
            map.put(tarId, reqList);
        }

        sort(map.values());
        return Collections.unmodifiableMap(map);
    }

    private void sort(Collection<List<Marriage>> list) {
        for (List<Marriage> l : list) {
            l.sort(Comparator.comparing(Marriage::getTimeCreated));
        }
    }

    private Map<Long, UserEconomy> fetchMappedEconomy(Set<Long> users) {
        return ecoRepo.findByIds(users)
                .stream()
                .collect(Collectors.toConcurrentMap(UserEconomy::getUserId, (b) -> b));
    }
}