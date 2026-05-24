package ofc.bot.handlers;

import ofc.bot.util.content.annotations.jobs.CronJob;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

public class SchedulerRegistryManager {

    /**
     * Registers the provided cron jobs into the {@link Scheduler}.
     * <p>
     * This method <b><u>DOES NOT</u></b> start the scheduler automatically,
     * you must call {@link #start} in order to fully initialize the cron jobs.
     *
     * @param jobs the cron jobs to be registered.
     * @throws SchedulerException if there is an error scheduling the job.
     */
    public static void initializeSchedulers(Job... jobs) throws SchedulerException {
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        Class<CronJob> jobAnn = CronJob.class;

        for (Job job : jobs) {
            if (!job.getClass().isAnnotationPresent(jobAnn))
                throw new IllegalStateException("Annotation '@" + jobAnn.getSimpleName() + "' must be present in cron job classes, please, include it at " + job.getClass().getName());

            JobDetail detail = getDetail(job);
            Trigger trigger = getTrigger(job);

            scheduler.scheduleJob(detail, trigger);
        }
    }

    /**
     * Starts the cron jobs registered with {@link #initializeSchedulers(Job...)}.
     *
     * @throws SchedulerException if {@link #stopSchedulers(boolean)} has
     *         been called, or there is an error starting the scheduler.
     */
    public static void start() throws SchedulerException {
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();
    }

    public static void stopSchedulers(boolean waitCompletion) throws SchedulerException {
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.shutdown(waitCompletion);
    }

    private static JobDetail getDetail(Job job) {
        Class<? extends Job> jobClass = job.getClass();
        CronJob jobAnnotation = jobClass.getDeclaredAnnotation(CronJob.class);

        if (jobAnnotation == null)
            throw new IllegalArgumentException(
                    "Job class " + jobClass.getName() + " must be annotated with " + CronJob.class.getName());

        String identity = jobAnnotation.identity();
        String name = identity.isBlank()
                ? jobClass.getSimpleName()
                : identity;

        return JobBuilder.newJob(jobClass)
                .withIdentity(name)
                .build();
    }

    private static Trigger getTrigger(Job job) {
        Class<? extends Job> jobClass = job.getClass();
        CronJob jobAnnotation = jobClass.getDeclaredAnnotation(CronJob.class);

        if (jobAnnotation == null)
            throw new IllegalArgumentException("Job class " + jobClass.getName() + " must be annotated with CronJob");

        String identity = jobAnnotation.identity();
        String name = identity.isBlank()
                ? jobClass.getSimpleName()
                : identity;

        return TriggerBuilder.newTrigger()
                .withIdentity(name)
                .withSchedule(CronScheduleBuilder.cronSchedule(jobAnnotation.expression()))
                .build();
    }
}