package ofc.bot.util.content.annotations.jobs;

import ofc.bot.handlers.SchedulerRegistryManager;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Classes with this annotation will be registered as cron jobs
 * by the {@link org.quartz.Scheduler#scheduleJob(JobDetail, Trigger) Scheduler.scheduleJob(JobDetail, Trigger)}
 * method, and must implement the {@link org.quartz.Job Job}
 * interface, otherwise, an {@link IllegalStateException} is thrown
 * by the {@link SchedulerRegistryManager SchedulerRegistryManager}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CronJob {

    /**
     * The cron expression used to trigger the {@link org.quartz.Job#execute(JobExecutionContext) Job.execute(JobExecutionContext)}
     * method from the implementor class.
     * <p>
     * You can use an <a href="https://www.freeformatter.com/cron-expression-generator-quartz.html">Expression Generator</a> for it.
     *
     * @return The Cron expression.
     */
    String expression();

    /**
     * The identity used at {@link org.quartz.JobBuilder#withIdentity(String) JobBuilder.withIdentity(String)}.
     *
     * @return the name element for the Job's JobKey.
     */
    String identity() default "";
}