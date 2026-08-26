package com.example.mealomat.feature.auth

import com.example.mealomat.core.AppError
import com.example.mealomat.core.Outcome
import com.example.mealomat.core.SignInError
import com.example.mealomat.testing.FakeAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class SignInViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(result: Outcome<Unit>) =
        SignInViewModel(FakeAuth("user-1", signInResult = result)).also {
            it.onEmailChange("nik@jomb.ch")
            it.onPasswordChange("hunter2")
        }

    @Test
    fun invalidCredentialsGetTheirOwnMessage() = runTest(dispatcher) {
        val vm = viewModel(Outcome.Fail(SignInError.InvalidCredentials))
        vm.submit()
        runCurrent()
        assertEquals("Email or password is incorrect.", vm.state.value.error)
        assertFalse(vm.state.value.isSubmitting)
    }

    @Test
    fun offlineSaysTheConnectionIsTheProblem() = runTest(dispatcher) {
        val vm = viewModel(Outcome.Fail(AppError.Offline))
        vm.submit()
        runCurrent()
        assertEquals("Can't reach the server. Signing in needs a connection.", vm.state.value.error)
    }

    @Test
    fun serverErrorsFallBackToTheGenericMessage() = runTest(dispatcher) {
        val vm = viewModel(Outcome.Fail(AppError.Server(500)))
        vm.submit()
        runCurrent()
        assertEquals("Something went wrong. Try again.", vm.state.value.error)
    }

    @Test
    fun successClearsTheError() = runTest(dispatcher) {
        val vm = viewModel(Outcome.Ok(Unit))
        vm.submit()
        runCurrent()
        assertNull(vm.state.value.error)
        assertFalse(vm.state.value.isSubmitting)
    }
}
