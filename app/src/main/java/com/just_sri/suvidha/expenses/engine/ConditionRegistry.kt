package com.just_sri.suvidha.expenses.engine

class ConditionRegistry {
    private val evaluators: MutableMap<String, suspend (RuleContext) -> ValidationResult> = mutableMapOf()
    private val actorEvaluators: MutableMap<String, suspend (RuleContext) -> ValidationResult> = mutableMapOf()

    /** Guards a rule's `actor:` value; it is checked before any of the rule's conditions. */
    fun registerActor(actorName: String, evaluator: suspend (RuleContext) -> ValidationResult): Unit {
        actorEvaluators[actorName] = evaluator
    }

    fun hasActor(actorName: String): Boolean {
        return actorEvaluators.containsKey(actorName)
    }

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
        val actorResult: ValidationResult = actorEvaluators[rule.actor]?.invoke(context) ?: ValidationResult.Allowed
        if (actorResult is ValidationResult.Denied) return actorResult
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
