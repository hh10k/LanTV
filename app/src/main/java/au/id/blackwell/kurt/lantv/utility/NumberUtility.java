package au.id.blackwell.kurt.lantv.utility;

public final class NumberUtility {
    private NumberUtility() {
    }

    public static float getProgressForUnboundedStages(int stage, float stageProgress) {
        // Each stage has a progress of half as much as the previous stage.
        // i.e. The first stage is 0..50%, the second is 50..75%, then 75..87%, etc.
        float range = (1.0f / (float)Math.pow(2.0, (double)stage));
        float start = (1.0f - 2.0f * range);
        return start + (range * (float)stageProgress / 100.0f);
    }
}
