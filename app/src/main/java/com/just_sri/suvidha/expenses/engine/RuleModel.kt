package com.just_sri.suvidha.expenses.engine

data class Rule(
    val id: String,
    val actor: String,
    val conditions: List<String> = emptyList(),
    val actions: List<String> = emptyList(),
    val onFailure: List<String> = emptyList(),
    val finallyActions: List<String> = emptyList(),
    val confirm: String? = null,
    val message: String? = null,
)

data class RuleContext(
    val groupId: String = "",
    val actorMemberId: String = "",
    val targetMemberId: String? = null,
    val isActorOwner: Boolean = false,
    val note: String? = null,
    val attributes: MutableMap<String, Any?> = mutableMapOf(),
)

sealed interface ValidationResult {
    data object Allowed : ValidationResult
    data class Denied(val condition: String, val reason: String) : ValidationResult
}

sealed interface ActionResult {
    data class Success(val message: String? = null) : ActionResult
    data class Failure(val reason: String, val cause: Throwable? = null) : ActionResult
}

sealed interface ExecutionResult {
    data class Success(val ruleId: String, val actionOutcomes: Map<String, ActionResult>) : ExecutionResult
    data class ValidationFailed(val ruleId: String, val violation: ValidationResult.Denied) : ExecutionResult
    data class ActionFailed(val ruleId: String, val failedAction: String, val reason: String) : ExecutionResult
}
