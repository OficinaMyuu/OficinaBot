package ofc.bot.jobs;

import ofc.bot.listeners.console.QueryCounter;
import ofc.bot.util.content.annotations.jobs.CronJob;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CronJob(expression = "0 0/15 * ? * * *")
public class QueryCountPrinter implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryCountPrinter.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        int queryCount = QueryCounter.poll();
        LOGGER.info("Executed {} queries in the last 15 minutes", queryCount);
    }
}