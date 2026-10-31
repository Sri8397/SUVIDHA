package com.just_sri.suvidha.expenses.engine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class RuleEngine(
    val conditions: ConditionRegistry,
    val actions: ActionRegistry,
    initialRules: Map<String, Rule> = emptyMap(),
) {
    @Volatile
    private var rules: Map<String, Rule> = initialRules.toMap()

    fun registerRule(rule: Rule): Unit {
        rules = rules + (rule.id to rule)
    }

    fun getRule(ruleId: String): Rule? {
        return rules[ruleId]
    }

    fun getAllRules(): List<Rule> {
        return rules.values.toList()
    }

    suspend fun canExecute(ruleId: String, context: RuleContext): ValidationResult {
        val rule: Rule = rules[ruleId] ?: return ValidationResult.Denied(ruleId, "Rule not found: $ruleId")
        return conditions.validate(rule, context)
    }

    suspend fun execute(ruleId: String, context: RuleContext): ExecutionResult {
        val rule: Rule = rules[ruleId] ?: return ExecutionResult.ActionFailed(ruleId, "lookup", "Rule not found: $ruleId")
        val validation: ValidationResult = conditions.validate(rule, context)
        if (validation is ValidationResult.Denied) {
            return ExecutionResult.ValidationFailed(ruleId, validation)
        }
        val outcomes: MutableMap<String, ActionResult> = mutableMapOf()
        var failureResult: ExecutionResult.ActionFailed? = null
        try {
            for (actionName in rule.actions) {
                val result: ActionResult = actions.execute(actionName, context)
                outcomes[actionName] = result
                if (result is ActionResult.Failure) {
                    failureResult = ExecutionResult.ActionFailed(ruleId, actionName, result.reason)
                    break
                }
            }
            if (failureResult != null) {
                for (failAction in rule.onFailure) {
                    outcomes[failAction] = actions.execute(failAction, context)
                }
                return failureResult
            }
            return ExecutionResult.Success(ruleId, outcomes)
        } catch (cancellation: CancellationException) {
            for (failAction in rule.onFailure) {
                runCatching { actions.execute(failAction, context) }
            }
            throw cancellation
        } catch (failure: Throwable) {
            for (failAction in rule.onFailure) {
                outcomes[failAction] = actions.execute(failAction, context)
            }
            return ExecutionResult.ActionFailed(ruleId, "exception", failure.message ?: "Execution failed")
        } finally {
            withContext(NonCancellable) {
                for (finallyAction in rule.finallyActions) {
                    val outcome = actions.execute(finallyAction, context)
                    outcomes[finallyAction] = outcome
                }
            }
        }
    }

    fun loadYaml(yamlContent: String, validateRegistries: Boolean = true): Unit {
        val parsedRules: Map<String, Rule> = parseYamlRules(yamlContent)
        if (validateRegistries) {
            parsedRules.values.forEach { rule: Rule ->
                rule.conditions.forEach { conditionName: String ->
                    check(conditions.hasCondition(conditionName)) {
                        "Unknown condition '$conditionName' declared in rule '${rule.id}'"
                    }
                }
                rule.actions.forEach { actionName: String ->
                    check(actions.hasAction(actionName)) {
                        "Unknown action '$actionName' declared in rule '${rule.id}'"
                    }
                }
                rule.onFailure.forEach { actionName: String ->
                    check(actions.hasAction(actionName)) {
                        "Unknown failure action '$actionName' declared in rule '${rule.id}'"
                    }
                }
                rule.finallyActions.forEach { actionName: String ->
                    check(actions.hasAction(actionName)) {
                        "Unknown finally action '$actionName' declared in rule '${rule.id}'"
                    }
                }
            }
        }
        rules = parsedRules
    }

    companion object {
        fun parseYamlRules(yamlContent: String): Map<String, Rule> {
            val result: MutableMap<String, Rule> = mutableMapOf()
            val lines: List<String> = yamlContent.lines()
            var currentRuleId: String? = null
            var actor: String = ""
            var confirm: String? = null
            var message: String? = null
            var currentSection: String? = null
            val conditions: MutableList<String> = mutableListOf()
            val actions: MutableList<String> = mutableListOf()
            val onFailure: MutableList<String> = mutableListOf()
            val finallyActions: MutableList<String> = mutableListOf()
            fun commitCurrentRule() {
                val ruleId: String? = currentRuleId
                if (ruleId != null && actor.isNotBlank()) {
                    result[ruleId] = Rule(
                        id = ruleId,
                        actor = actor,
                        conditions = conditions.toList(),
                        actions = actions.toList(),
                        onFailure = onFailure.toList(),
                        finallyActions = finallyActions.toList(),
                        confirm = confirm,
                        message = message,
                    )
                }
            }
            lines.forEach { rawLine: String ->
                val line: String = rawLine.trimEnd()
                val trimmed: String = line.trim()
                if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                    val indent: Int = rawLine.takeWhile { it == ' ' }.length
                    if (indent == 2 && line.endsWith(":") && !trimmed.startsWith("-")) {
                        commitCurrentRule()
                        currentRuleId = trimmed.removeSuffix(":")
                        actor = ""
                        confirm = null
                        message = null
                        currentSection = null
                        conditions.clear()
                        actions.clear()
                        onFailure.clear()
                        finallyActions.clear()
                    } else if (currentRuleId != null) {
                        if (trimmed.startsWith("actor:")) {
                            actor = parseScalarValue(trimmed.removePrefix("actor:"))
                            currentSection = null
                        } else if (trimmed.startsWith("confirm:")) {
                            confirm = parseNullableScalar(trimmed.removePrefix("confirm:"))
                            currentSection = null
                        } else if (trimmed.startsWith("message:")) {
                            message = parseNullableScalar(trimmed.removePrefix("message:"))
                            currentSection = null
                        } else if (trimmed == "conditions:" || trimmed == "conditions: []") {
                            currentSection = "conditions"
                        } else if (trimmed == "actions:" || trimmed == "actions: []") {
                            currentSection = "actions"
                        } else if (trimmed == "on_failure:" || trimmed == "on_failure: []") {
                            currentSection = "on_failure"
                        } else if (trimmed == "finally:" || trimmed == "finally: []") {
                            currentSection = "finally"
                        } else if (trimmed.startsWith("- ")) {
                            val item: String = trimmed.removePrefix("- ").trim()
                            when (currentSection) {
                                "conditions" -> conditions.add(item)
                                "actions" -> actions.add(item)
                                "on_failure" -> onFailure.add(item)
                                "finally" -> finallyActions.add(item)
                            }
                        }
                    }
                }
            }
            commitCurrentRule()
            return result
        }

        private fun parseScalarValue(raw: String): String {
            val clean: String = raw.trim().trim('"', '\'')
            return clean
        }

        private fun parseNullableScalar(raw: String): String? {
            val clean: String = raw.trim()
            if (clean == "null" || clean == "~" || clean.isBlank()) return null
            return clean.trim('"', '\'')
        }
    }
}
