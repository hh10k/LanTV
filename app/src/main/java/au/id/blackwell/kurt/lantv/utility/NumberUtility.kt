package au.id.blackwell.kurt.lantv.utility

object NumberUtility {

    fun getProgressForUnboundedStages(stage: Int, stageProgress: Float): Float {
        // Each stage has a progress of half as much as the previous stage.
        // i.e. The first stage is 0..50%, the second is 50..75%, then 75..87%, etc.
        val range = 1.0f / Math.pow(2.0, stage.toDouble()).toFloat()
        val start = 1.0f - 2.0f * range
        return start + range * stageProgress.toFloat() / 100.0f
    }
}
