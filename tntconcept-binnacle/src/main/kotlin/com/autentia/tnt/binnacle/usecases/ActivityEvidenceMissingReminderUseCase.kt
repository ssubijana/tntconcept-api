package com.autentia.tnt.binnacle.usecases

import com.autentia.tnt.binnacle.core.domain.DateInterval
import com.autentia.tnt.binnacle.entities.*
import com.autentia.tnt.binnacle.repositories.ActivityRepository
import com.autentia.tnt.binnacle.repositories.predicates.ActivityPredicates.belongsToUsers
import com.autentia.tnt.binnacle.repositories.predicates.ActivityPredicates.missingEvidenceOnce
import com.autentia.tnt.binnacle.repositories.predicates.ActivityPredicates.missingEvidenceWeekly
import com.autentia.tnt.binnacle.repositories.predicates.ActivityPredicates.startDateBetweenDates
import com.autentia.tnt.binnacle.repositories.predicates.PredicateBuilder.and
import com.autentia.tnt.binnacle.repositories.predicates.PredicateBuilder.or
import com.autentia.tnt.binnacle.services.ActivityEvidenceMissingMailService
import com.autentia.tnt.binnacle.services.UserService
import io.micronaut.data.jpa.repository.criteria.Specification
import io.micronaut.transaction.annotation.ReadOnly
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.time.LocalDate
import java.util.*
import javax.transaction.Transactional

@Singleton
class ActivityEvidenceMissingReminderUseCase @Inject internal constructor(
    @param:Named("Internal") private val activityRepository: ActivityRepository,
    private val activityEvidenceMissingMailService: ActivityEvidenceMissingMailService,
    private val userService: UserService,
) {

    @Transactional
    @ReadOnly
    fun sendReminders() {
        val rolesMissingEvidenceByUser: Map<User, List<ProjectRole>> = getProjectRolesMissingEvidenceByUser()

        rolesMissingEvidenceByUser.forEach { (user, rolesMissingEvidence) ->
            notifyMissingEvidencesToUser(user, rolesMissingEvidence)
        }
    }

    private fun getProjectRolesMissingEvidenceByUser(): Map<User, List<ProjectRole>> {
        val activeUsers: List<User> = userService.findActive()

        val activitiesMissingEvidence: List<Activity> = activityRepository.findAll(
            getActivitiesMissingEvidencePredicate(activeUsers.map { it.id }.toList())
        )

        val activitiesMissingEvidenceByUser = mutableMapOf<User, List<Activity>>()
        for (user in activeUsers) {
            activitiesMissingEvidenceByUser[user] = activitiesMissingEvidence.filter { it.userId == user.id }
        }

        return activitiesMissingEvidenceByUser.mapValues { activitiesByUser ->
            activitiesByUser.value.map { it.projectRole }.distinct()
        }
    }

    private fun getActivitiesMissingEvidencePredicate(listOfUserIds: List<Long>): Specification<Activity> {
        val dateInterval = DateInterval.of(LocalDate.now().minusDays(7), LocalDate.now())
        return and(
            or(
                missingEvidenceOnce(),
                and(missingEvidenceWeekly(), startDateBetweenDates(dateInterval))
            ),
            belongsToUsers(listOfUserIds)
        )
    }

    private fun notifyMissingEvidencesToUser(user: User, rolesMissingEvidence: List<ProjectRole>) {
        val locale = Locale.forLanguageTag("es")
        rolesMissingEvidence.forEach {
            activityEvidenceMissingMailService.sendEmail(
                it.project.organization.name,
                it.project.name,
                it.name,
                it.requireEvidence,
                user.email,
                locale
            )
        }
    }
}