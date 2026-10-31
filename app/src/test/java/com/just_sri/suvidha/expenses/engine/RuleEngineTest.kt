package com.just_sri.suvidha.expenses.engine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RuleEngineTest {

    private val sampleYaml: String = """
version: "1.2"
category: group_membership

rules:
  self_exit:
    actor: active_member
    conditions:
      - owner_must_transfer_or_close
    confirm: "Leave group?"
    actions:
      - cancel_authored_active_splits
      - request_mls_self_removal
      - sync_causal_ledger_event
    message: "{member} left the group"

  admin_removal:
    actor: owner_only
    conditions:
      - net_balance_zero
      - no_outstanding_pairwise_debts
    confirm: "Remove member?"
    actions:
      - mls_remove_member
      - sync_causal_ledger_event
    message: "{member} was removed by admin"
"""

    @Test
    fun parseYamlRulesExtractsAllFieldsAccurately() {
        val rules: Map<String, Rule> = RuleEngine.parseYamlRules(sampleYaml)
        assertEquals(2, rules.size)
        val selfExit: Rule? = rules["self_exit"]
        assertNotNull(selfExit)
        assertEquals("active_member", selfExit?.actor)
        assertEquals(listOf("owner_must_transfer_or_close"), selfExit?.conditions)
        assertEquals(listOf("cancel_authored_active_splits", "request_mls_self_removal", "sync_causal_ledger_event"), selfExit?.actions)
        assertEquals("Leave group?", selfExit?.confirm)
        assertEquals("{member} left the group", selfExit?.message)
        val adminRemoval: Rule? = rules["admin_removal"]
        assertNotNull(adminRemoval)
        assertEquals("owner_only", adminRemoval?.actor)
        assertEquals(listOf("net_balance_zero", "no_outstanding_pairwise_debts"), adminRemoval?.conditions)
        assertEquals(listOf("mls_remove_member", "sync_causal_ledger_event"), adminRemoval?.actions)
        assertEquals("Remove member?", adminRemoval?.confirm)
        assertEquals("{member} was removed by admin", adminRemoval?.message)
    }

    @Test
    fun loadRulesDirectlyFromActualYamlFile() {
        val yamlContent: String = File("app/rules/group_membership.yaml").takeIf { it.exists() }?.readText()
            ?: File("rules/group_membership.yaml").takeIf { it.exists() }?.readText()
            ?: error("group_membership.yaml file not found")
        val rules: Map<String, Rule> = RuleEngine.parseYamlRules(yamlContent)
        val expectedRuleIds: Set<String> = setOf(
            "self_exit", "admin_removal", "transfer_ownership", "close_group", "rename_group",
            "invite_member", "cancel_split", "delete_debt", "invitation_enabled", "invitation_disabled",
            "create_group", "post_message", "add_split", "record_payment",
        )
        assertEquals(expectedRuleIds, rules.keys)
        val expectedActors: Set<String> = setOf("active_member", "owner_only", "user")
        assertTrue(rules.values.all { it.actor in expectedActors })
    }

    @Test
    fun loadRefreshRulesDirectlyFromActualYamlFile() {
        val yamlContent: String = File("app/rules/refresh.yaml").takeIf { it.exists() }?.readText()
            ?: File("rules/refresh.yaml").takeIf { it.exists() }?.readText()
            ?: error("refresh.yaml file not found")
        val rules: Map<String, Rule> = RuleEngine.parseYamlRules(yamlContent)
        assertTrue(rules.containsKey("manual_pull_refresh"))
        assertTrue(rules.containsKey("background_idle_sync"))
        assertTrue(rules.containsKey("scheduled_retry_sync"))
        assertTrue(rules.containsKey("inbound_expense_notification"))
        assertTrue(rules.containsKey("inbound_settlement_notification"))
        val pullRefresh = rules["manual_pull_refresh"]
        assertEquals("user", pullRefresh?.actor)
        assertTrue(pullRefresh?.actions?.contains("execute_parallel_relay_fetches") == true)
        assertTrue(pullRefresh?.onFailure?.contains("schedule_background_retry_if_needed") == true)
        assertTrue(pullRefresh?.finallyActions?.contains("clear_busy_indicator") == true)
    }

    @Test
    fun ruleEngineExecutesFinallyAndOnFailureOnActionFailure() = runBlocking {
        val conditions = ConditionRegistry()
        val actions = ActionRegistry()
        var failureExecuted = false
        var finallyExecuted = false
        actions.register("failing_action") { ActionResult.Failure("Simulated failure") }
        actions.registerUnit("on_fail_hook") { failureExecuted = true }
        actions.registerUnit("finally_hook") { finallyExecuted = true }

        val yaml = """
rules:
  test_rule:
    actor: user
    actions:
      - failing_action
    on_failure:
      - on_fail_hook
    finally:
      - finally_hook
"""
        val engine = RuleEngine(conditions, actions)
        engine.loadYaml(yaml, validateRegistries = true)
        val result = engine.execute("test_rule", RuleContext())

        assertTrue(result is ExecutionResult.ActionFailed)
        assertTrue(failureExecuted)
        assertTrue(finallyExecuted)
    }

    @Test
    fun loadYamlReplacesRulesAtomically() {
        val conditions = ConditionRegistry()
        val actions = ActionRegistry()
        conditions.registerPredicate("condition_a", "Fail") { true }
        conditions.registerPredicate("condition_b", "Fail") { true }
        actions.registerUnit("action_a") {}
        actions.registerUnit("action_b") {}
        val engine = RuleEngine(conditions, actions)
        val initialYaml = """
rules:
  rule_one:
    actor: user
    conditions:
      - condition_a
    actions:
      - action_a
"""
        engine.loadYaml(initialYaml, validateRegistries = true)
        assertNotNull(engine.getRule("rule_one"))
        val replacementYaml = """
rules:
  rule_two:
    actor: user
    conditions:
      - condition_b
    actions:
      - action_b
"""
        engine.loadYaml(replacementYaml, validateRegistries = true)
        assertNull(engine.getRule("rule_one"))
        assertNotNull(engine.getRule("rule_two"))
    }

    @Test
    fun executeHaltsWhenConditionFails() = runBlocking {
        val testYaml = """
rules:
  admin_removal:
    actor: owner_only
    conditions:
      - net_balance_zero
    actions:
      - mls_remove_member
"""
        val conditions = ConditionRegistry()
        val actions = ActionRegistry()
        conditions.registerPredicate("net_balance_zero", "Balance is not zero") { false }
        actions.registerUnit("mls_remove_member") {}
        val engine = RuleEngine(conditions, actions)
        engine.loadYaml(testYaml, validateRegistries = true)
        val inputContext = RuleContext(groupId = "group-1", actorMemberId = "owner-1", targetMemberId = "member-2")
        val actualResult: ExecutionResult = engine.execute("admin_removal", inputContext)
        assertTrue(actualResult is ExecutionResult.ValidationFailed)
        val failure: ExecutionResult.ValidationFailed = actualResult as ExecutionResult.ValidationFailed
        assertEquals("net_balance_zero", failure.violation.condition)
    }

    @Test
    fun executeRunsActionsInOrderWhenConditionsPass() = runBlocking {
        val testYaml = """
rules:
  self_exit:
    actor: active_member
    conditions:
      - owner_must_transfer_or_close
    actions:
      - cancel_authored_active_splits
      - request_mls_self_removal
      - sync_causal_ledger_event
"""
        val conditions = ConditionRegistry()
        val actions = ActionRegistry()
        conditions.registerPredicate("owner_must_transfer_or_close", "Cannot exit") { true }
        val executedActions: MutableList<String> = mutableListOf()
        actions.registerUnit("cancel_authored_active_splits") { executedActions.add("cancel_authored_active_splits") }
        actions.registerUnit("request_mls_self_removal") { executedActions.add("request_mls_self_removal") }
        actions.registerUnit("sync_causal_ledger_event") { executedActions.add("sync_causal_ledger_event") }
        val engine = RuleEngine(conditions, actions)
        engine.loadYaml(testYaml, validateRegistries = true)
        val inputContext = RuleContext(groupId = "group-1", actorMemberId = "member-1")
        val actualResult: ExecutionResult = engine.execute("self_exit", inputContext)
        assertTrue(actualResult is ExecutionResult.Success)
        assertEquals(3, executedActions.size)
        assertEquals("cancel_authored_active_splits", executedActions[0])
        assertEquals("request_mls_self_removal", executedActions[1])
        assertEquals("sync_causal_ledger_event", executedActions[2])
    }

    @Test(expected = CancellationException::class)
    fun actionRegistryRethrowsCancellationException() = runBlocking {
        val actions = ActionRegistry()
        actions.registerUnit("cancel_action") {
            throw CancellationException("Coroutine was cancelled")
        }
        val context = RuleContext(groupId = "group-1", actorMemberId = "member-1")
        actions.execute("cancel_action", context)
        Unit
    }

    @Test(expected = IllegalStateException::class)
    fun loadYamlRejectsUnknownCondition() {
        val conditions = ConditionRegistry()
        val actions = ActionRegistry()
        actions.registerUnit("cancel_authored_active_splits") {}
        val invalidYaml = """
rules:
  test_rule:
    actor: active_member
    conditions:
      - non_existent_condition
    actions:
      - cancel_authored_active_splits
"""
        val engine = RuleEngine(conditions, actions)
        engine.loadYaml(invalidYaml, validateRegistries = true)
    }

    @Test(expected = IllegalStateException::class)
    fun loadYamlRejectsUnknownAction() {
        val conditions = ConditionRegistry()
        val actions = ActionRegistry()
        conditions.registerPredicate("known_condition", "Reason") { true }
        val invalidYaml = """
rules:
  test_rule:
    actor: active_member
    conditions:
      - known_condition
    actions:
      - non_existent_action
"""
        val engine = RuleEngine(conditions, actions)
        engine.loadYaml(invalidYaml, validateRegistries = true)
    }

    private val ownerOnlyYaml: String = """
rules:
  rename_group:
    actor: owner_only
    conditions:
      - group_name_valid
    actions:
      - publish_group_rename
"""

    @Test
    fun executeDeniesWhenActorEvaluatorDenies() = runBlocking {
        val conditions = ConditionRegistry()
        val actions = ActionRegistry()
        val executedActions: MutableList<String> = mutableListOf()
        conditions.registerActor("owner_only") { ValidationResult.Denied("owner_only", "Only the group owner can do this.") }
        conditions.registerPredicate("group_name_valid", "Invalid name") { true }
        actions.registerUnit("publish_group_rename") { executedActions.add("publish_group_rename") }
        val engine = RuleEngine(conditions, actions, requireActors = true)
        engine.loadYaml(ownerOnlyYaml, validateRegistries = true)
        val inputContext = RuleContext(groupId = "group-1", actorMemberId = "member-1", note = "Trip")
        val actualResult: ExecutionResult = engine.execute("rename_group", inputContext)
        assertTrue(actualResult is ExecutionResult.ValidationFailed)
        assertEquals("owner_only", (actualResult as ExecutionResult.ValidationFailed).violation.condition)
        assertTrue(executedActions.isEmpty())
    }

    @Test
    fun actorIsCheckedBeforeConditions() = runBlocking {
        val conditions = ConditionRegistry()
        val actions = ActionRegistry()
        var isConditionEvaluated = false
        conditions.registerActor("owner_only") { ValidationResult.Denied("owner_only", "Only the group owner can do this.") }
        conditions.registerPredicate("group_name_valid", "Invalid name") { isConditionEvaluated = true; true }
        actions.registerUnit("publish_group_rename") {}
        val engine = RuleEngine(conditions, actions, requireActors = true)
        engine.loadYaml(ownerOnlyYaml, validateRegistries = true)
        val actualResult: ValidationResult = engine.canExecute("rename_group", RuleContext(groupId = "group-1", actorMemberId = "member-1"))
        assertTrue(actualResult is ValidationResult.Denied)
        assertTrue(!isConditionEvaluated)
    }

    @Test(expected = IllegalStateException::class)
    fun loadYamlRejectsUnknownActorWhenActorsRequired() {
        val conditions = ConditionRegistry()
        val actions = ActionRegistry()
        conditions.registerPredicate("group_name_valid", "Invalid name") { true }
        actions.registerUnit("publish_group_rename") {}
        val engine = RuleEngine(conditions, actions, requireActors = true)
        engine.loadYaml(ownerOnlyYaml, validateRegistries = true)
    }

    @Test
    fun groupMembershipYamlUsesOnlyRegisteredActors() {
        val yamlContent: String = File("app/rules/group_membership.yaml").takeIf { it.exists() }?.readText()
            ?: File("rules/group_membership.yaml").takeIf { it.exists() }?.readText()
            ?: error("group_membership.yaml file not found")
        val conditions = ConditionRegistry()
        val actions = ActionRegistry()
        listOf("active_member", "owner_only", "user").forEach { actorName: String -> conditions.registerActor(actorName) { ValidationResult.Allowed } }
        val engine = RuleEngine(conditions, actions, requireActors = true)
        engine.loadYaml(yamlContent, validateRegistries = false)
        assertEquals(14, engine.getAllRules().size)
    }

    @Test
    fun ledgerRulesDeclareExpectedActorsConditionsAndActions() {
        val yamlContent: String = File("app/rules/group_membership.yaml").takeIf { it.exists() }?.readText()
            ?: File("rules/group_membership.yaml").takeIf { it.exists() }?.readText()
            ?: error("group_membership.yaml file not found")
        val actualRules: Map<String, Rule> = RuleEngine.parseYamlRules(yamlContent)
        assertEquals("user", actualRules.getValue("create_group").actor)
        assertEquals(listOf("create_mls_group"), actualRules.getValue("create_group").actions)
        assertEquals("active_member", actualRules.getValue("post_message").actor)
        assertEquals(listOf("message_text_valid"), actualRules.getValue("post_message").conditions)
        assertEquals(listOf("split_participants_are_active_members"), actualRules.getValue("add_split").conditions)
        assertEquals(listOf("actor_is_outstanding_debt_creditor", "payment_amount_within_balance"), actualRules.getValue("record_payment").conditions)
        assertEquals(listOf("publish_settlement"), actualRules.getValue("record_payment").actions)
    }
}
