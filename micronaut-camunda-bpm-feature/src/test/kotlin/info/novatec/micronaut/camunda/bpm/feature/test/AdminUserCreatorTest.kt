/*
 * Copyright 2020-2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.novatec.micronaut.camunda.bpm.feature.test

import info.novatec.micronaut.camunda.bpm.feature.Configuration
import info.novatec.micronaut.camunda.bpm.feature.AdminUserCreator
import io.micronaut.core.value.PropertyNotFoundException
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.runtime.server.event.ServerStartupEvent
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import org.camunda.bpm.engine.ProcessEngine
import org.camunda.bpm.engine.authorization.Authorization.ANY
import org.camunda.bpm.engine.authorization.Groups.CAMUNDA_ADMIN
import org.camunda.bpm.engine.authorization.Groups.GROUP_TYPE_SYSTEM
import org.camunda.bpm.engine.authorization.Resources
import org.camunda.bpm.engine.identity.User
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import java.util.*

/**
 * Tests for [info.novatec.micronaut.camunda.bpm.feature.AdminUserCreator]
 *
 * @author Titus Meyer
 */
class AdminUserCreatorTest {
    @MicronautTest
    @Nested
    inner class AdminUserCreatorTestWithoutProperties {
        @Inject
        lateinit var processEngine: ProcessEngine

        @Inject
        lateinit var configuration: Configuration

        @Inject
        lateinit var adminUserCreator: Optional<AdminUserCreator>

        @Test
        fun adminUserNotDefined() {
            assertThrows(PropertyNotFoundException::class.java) { configuration.adminUser.id }
            assertFalse(adminUserCreator.isPresent)
            assertEquals(0, processEngine.identityService.createUserQuery().count())
        }
    }

    @MicronautTest
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AdminUserCreatorTestWithAllProperties : TestPropertyProvider {

        override fun getProperties(): MutableMap<String, String> {
            return mutableMapOf(
                "camunda.admin-user.id" to "admin",
                "camunda.admin-user.password" to "admin",
                "camunda.admin-user.firstname" to "Donald",
                "camunda.admin-user.lastname" to "Duck",
                "camunda.admin-user.email" to "Donald.Duck@example.org",
            )
        }

        @Inject
        lateinit var processEngine: ProcessEngine

        @Inject
        lateinit var configuration: Configuration

        @Inject
        lateinit var adminUserCreator: Optional<AdminUserCreator>

        @Test
        fun adminUserCreated() {
            assertTrue(adminUserCreator.isPresent)

            assertEquals("admin", configuration.adminUser.id)
            assertEquals("admin", configuration.adminUser.password)
            assertEquals("Donald", configuration.adminUser.firstname.get())
            assertEquals("Duck", configuration.adminUser.lastname.get())
            assertEquals("Donald.Duck@example.org", configuration.adminUser.email.get())

            assertAdminUserExists(processEngine, configuration.adminUser.id)
            val adminUser = queryUser(processEngine, configuration.adminUser.id)
            assertEquals("Donald", adminUser.firstName)
            assertEquals("Duck", adminUser.lastName)
            assertEquals("Donald.Duck@example.org", adminUser.email)
            assertAdminGroupExists(processEngine)
            assertAdminGroupAuthorizationsExist(processEngine)
        }

        @Test
        fun adminUserOnlyCreatedOnce() {
            assertTrue(adminUserCreator.isPresent)

            assertEquals(1, processEngine.identityService.createUserQuery().count())
            assertAdminUserExists(processEngine, configuration.adminUser.id)
            assertAdminGroupExists(processEngine)
            assertAdminGroupAuthorizationsExist(processEngine)

            // Trigger event again and check that it is idempotent
            adminUserCreator.get().onApplicationEvent(ServerStartupEvent(mock(EmbeddedServer::class.java)))

            assertEquals(1, processEngine.identityService.createUserQuery().count())
            assertAdminUserExists(processEngine, configuration.adminUser.id)
            assertAdminGroupExists(processEngine)
            assertAdminGroupAuthorizationsExist(processEngine)
        }
    }

    @MicronautTest
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AdminUserCreatorTestWithOnlyRequiredProperties : TestPropertyProvider {

        override fun getProperties(): MutableMap<String, String> {
            return mutableMapOf(
                "camunda.admin-user.id" to "admin2",
                "camunda.admin-user.password" to "admin2",
            )
        }

        @Inject
        lateinit var processEngine: ProcessEngine

        @Inject
        lateinit var configuration: Configuration

        @Inject
        lateinit var adminUserCreator: Optional<AdminUserCreator>

        @Test
        fun adminUserCreated() {
            assertTrue(adminUserCreator.isPresent)

            assertEquals("admin2", configuration.adminUser.id)
            assertEquals("admin2", configuration.adminUser.password)

            assertAdminUserExists(processEngine, configuration.adminUser.id)
            val adminUser = queryUser(processEngine, configuration.adminUser.id)
            assertEquals("Admin2", adminUser.firstName)
            assertEquals("Admin2", adminUser.lastName)
            assertEquals("admin2@localhost", adminUser.email)
            assertAdminGroupExists(processEngine)
            assertAdminGroupAuthorizationsExist(processEngine)
        }

    }

    fun queryUser(processEngine: ProcessEngine, userId: String): User {
        return processEngine.identityService.createUserQuery().userId(userId).singleResult()
    }

    fun assertAdminUserExists(processEngine: ProcessEngine, userId: String) {
        val adminUser = queryUser(processEngine, userId)
        assertNotNull(adminUser)
        assertEquals(userId, adminUser.id)
    }

    fun assertAdminGroupExists(processEngine: ProcessEngine) {
        val adminGroup = processEngine.identityService.createGroupQuery().groupId(CAMUNDA_ADMIN).singleResult()
        assertNotNull(adminGroup)
        assertEquals(GROUP_TYPE_SYSTEM, adminGroup.type)
    }

    fun assertAdminGroupAuthorizationsExist(processEngine: ProcessEngine) {
        for (resource in Resources.values()) {
            assertEquals(
                1,
                processEngine.authorizationService.createAuthorizationQuery()
                    .groupIdIn(CAMUNDA_ADMIN).resourceType(resource).resourceId(ANY).count()
            )
        }
    }

}