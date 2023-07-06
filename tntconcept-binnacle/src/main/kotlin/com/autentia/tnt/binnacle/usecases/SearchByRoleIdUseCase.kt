package com.autentia.tnt.binnacle.usecases

import com.autentia.tnt.binnacle.converters.ProjectRoleConverter
import com.autentia.tnt.binnacle.converters.SearchConverter
import com.autentia.tnt.binnacle.core.domain.TimeInterval
import com.autentia.tnt.binnacle.entities.Activity
import com.autentia.tnt.binnacle.entities.dto.SearchResponseDTO
import com.autentia.tnt.binnacle.repositories.ActivityRepository
import com.autentia.tnt.binnacle.repositories.ProjectRoleRepository
import com.autentia.tnt.binnacle.services.ActivityCalendarService
import com.autentia.tnt.security.application.checkAuthentication
import com.autentia.tnt.security.application.id
import io.micronaut.security.utils.SecurityService
import jakarta.inject.Singleton
import java.time.LocalDate

@Singleton
class SearchByRoleIdUseCase internal constructor(
    private val projectRoleRepository: ProjectRoleRepository,
    private val activityRepository: ActivityRepository,
    private val securityService: SecurityService,
    private val activityCalendarService: ActivityCalendarService,
    private val projectRoleConverter: ProjectRoleConverter,
    private val searchConverter: SearchConverter,

    ) {

    private fun getTimeInterval(year: Int?) = TimeInterval.ofYear(year ?: LocalDate.now().year)

    fun get(roleIds: List<Long>, year: Int?): SearchResponseDTO {
        val authentication = securityService.checkAuthentication()
        val userId = authentication.id()
        val projectRoleIds = roleIds.distinct()

        val timeInterval = getTimeInterval(year)
        val projectRoles = projectRoleRepository.getAllByIdIn(projectRoleIds).map { it.toDomain() }
        val activities = activityRepository.findByProjectRoleIds(timeInterval.start, timeInterval.end, projectRoleIds, userId)
            .map(Activity::toDomain)

        val projectRoleUsers = projectRoles.map { projectRole ->
            val remainingOfProjectRole = activityCalendarService.getRemainingOfProjectRoleForUser(
                projectRole, activities, timeInterval.getDateInterval(), userId
            )
            projectRoleConverter.toProjectRoleUser(projectRole, remainingOfProjectRole, userId)
        }
        return searchConverter.toResponseDTO(projectRoles, projectRoleUsers)
    }
}