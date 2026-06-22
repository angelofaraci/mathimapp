package com.example.proyectofinal.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserRoleTest {
    @Test
    fun `parse accepts legacy learner value`() {
        assertEquals(UserRole.STUDENT, UserRole.parse("LEARNER"))
    }

    @Test
    fun `parse accepts canonical student value`() {
        assertEquals(UserRole.STUDENT, UserRole.parse("STUDENT"))
    }

    @Test
    fun `parse returns null for unknown values`() {
        assertNull(UserRole.parse("GUEST"))
    }
}
