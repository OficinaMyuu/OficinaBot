package ofc.bot.handlers.accumulator;

import java.util.List;

public record AccumulatorApprovalReport(
        boolean successful,
        int requested,
        int paid,
        long elapsedMillis,
        String summary,
        List<String> details
) {
    public static AccumulatorApprovalReport success(int requested, long elapsedMillis, String summary, List<String> details) {
        return new AccumulatorApprovalReport(true, requested, requested, elapsedMillis, summary, details);
    }

    public static AccumulatorApprovalReport failure(int requested, long elapsedMillis, String summary, List<String> details) {
        return new AccumulatorApprovalReport(false, requested, 0, elapsedMillis, summary, details);
    }
}
