package com.just_sri.suvidha.expenses.engine

class ConditionRegistry {
    private val evaluators: MutableMap<String, suspend (RuleContext) -> ValidationResult> = mutableMapOf()

    fun register(conditionName: String, evaluator: suspend (RuleContext) -> ValidationResult): Unit {
        evaluators[conditionName] = evaluator
    }

    fun registerPredicate(conditionName: String, failureReason: String, predicate: suspend (RuleContext) -> Boolean): Unit {
        evaluators[conditionName] = { context: RuleContext ->
            val isAllowed: Boolean = predicate(context)
            if (isAllowed) ValidationResult.Allowed else ValidationResult.Denied(conditionName, failureReason)
        }
    }

    suspend fun validate(rule: Rule, context: RuleContext): ValidationResult {
        rule.conditions.forEach { conditionName: String ->
            val evaluator = evaluators[conditionName] ?: return ValidationResult.Denied(conditionName, "Missing evaluator for condition: $conditionName")
            val result: ValidationResult = evaluator(context)
            if (result is ValidationResult.Denied) return result
        }
        return ValidationResult.Allowed
    }

    fun hasCondition(conditionName: String): Boolean {
        return evaluators.containsKey(conditionName)
    }
}
