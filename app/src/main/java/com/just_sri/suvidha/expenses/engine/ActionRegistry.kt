package com.just_sri.suvidha.expenses.engine

import kotlinx.coroutines.CancellationException

class ActionRegistry {
    private val handlers: MutableMap<String, suspend (RuleContext) -> ActionResult> = mutableMapOf()

    fun register(actionName: String, handler: suspend (RuleContext) -> ActionResult): Unit {
        handlers[actionName] = handler
    }

    fun registerUnit(actionName: String, handler: suspend (RuleContext) -> Unit): Unit {
        handlers[actionName] = { context: RuleContext ->
            try {
                handler(context)
                ActionResult.Success()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (failure: Exception) {
                ActionResult.Failure("Failed to execute $actionName: ${failure.message}", failure)
            }
        }
    }

    suspend fun execute(actionName: String, context: RuleContext): ActionResult {
        val handler = handlers[actionName] ?: return ActionResult.Failure("No handler registered for action: $actionName")
        return try {
            handler(context)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (failure: Exception) {
            ActionResult.Failure("Exception during $actionName: ${failure.message}", failure)
        }
    }

    fun hasAction(actionName: String): Boolean {
        return handlers.containsKey(actionName)
    }
}
