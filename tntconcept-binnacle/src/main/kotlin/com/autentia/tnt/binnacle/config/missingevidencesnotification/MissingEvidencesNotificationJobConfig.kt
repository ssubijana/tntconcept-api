package com.autentia.tnt.binnacle.config.missingevidencesnotification

import com.autentia.tnt.AppProperties
import com.autentia.tnt.binnacle.usecases.ActivityEvidenceMissingReminderUseCase
import io.micronaut.context.annotation.Context
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.TaskScheduler
import jakarta.inject.Named


@Context
internal class MissingEvidencesNotificationJobConfig(
    private val appProperties: AppProperties,
    private val activityEvidenceMissingReminderUseCase: ActivityEvidenceMissingReminderUseCase,
    @Named(TaskExecutors.SCHEDULED) taskScheduler: TaskScheduler
) {
    init {
        if (appProperties.binnacle.missingEvidencesNotification.cronExpression != null) {
            taskScheduler.schedule(appProperties.binnacle.missingEvidencesNotification.cronExpression) {
                activityEvidenceMissingReminderUseCase.sendReminders()
            }
        }
    }
}