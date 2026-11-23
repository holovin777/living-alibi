# Living Alibi v1.11 — Economy & Debts

## Scope

This version introduces a small, **rule-based peer lending economy**. It is not a full property/inventory, banking, employment-market or debt collection simulator. The system never calls OpenAI during simulation.

Only procedurally generated residents (not the existing Marco or Elena physiology loops) receive new periodic household bills. This avoids double-charging Marco, whose finances have been modelled separately since v1.0.

## Autonomy

- At 08:00, every generated resident receives housing and utility charges once each seven-day cycle, with a staggered due day. A smaller household repair may arrive once per 21-day cycle.
- Bills are paid only from money that exceeds a EUR 20 reserve. Any unpaid amount is tracked as an explicit arrear, never an implicitly negative bank balance.
- At selected daytime intervals, a generated citizen with cash below EUR 50 or outstanding bills can ask a trusted, co-present resident for up to EUR 70. The lender retains at least EUR 100 and must have a recorded relationship with sufficient trust and low tension.
- New peer loans are interest-free and due seven days after they originate. A borrower cannot stack loans. Each morning, available funds beyond EUR 90 may repay principal. If repayment has not finished after the due date, the contract becomes overdue and friendship trust is reduced once, with greater tension.
- Lender and borrower cash move by equal and opposite amounts for loans and repayments. Bills are a deliberate money sink paid to an external housing provider, not peer transfers.
- Overdue bills and active loan balances influence future **financial strain** in `MotiveLife`. Pre-existing investigative backgrounds remain immutable.

## Privacy, provenance and persistence

The engine's authoritative ledger (`economy_ledger`) is separate from public witness memory. At the moment of lending, bystanders may observe that a financial conversation occurred, but are **not told the loan amount or contract terms**. These are developer diagnostics:

```text
finances resident-0001
debts
ledger 20
ledger resident-0001 20
```

Do **not** use `finances`, `debts` or `ledger` to gain an unfair advantage in an investigation. The detective's `background` command continues to show only broad indicators from the time of the incident.

New SQLite fields/tables:

- `town_people.unpaid_bills INTEGER NOT NULL DEFAULT 0`
- `economy_loans`: stable contract ID, lender, borrower, initial/outstanding principal, creation/due dates, and status
- `economy_ledger`: append-only bill, payment, loan, repayment and overdue entries with timestamp and actor IDs

Existing databases migrate without discarding saved residents, relationships or investigative cases. No historical debts are reconstructed from previous balances. Contracts and ledger inserts are committed in the same transaction as the world tick. The pending ledger is cleared only after successful commit, avoiding duplicate entries on subsequent saves.

## Tests

```bash
mvn -q clean test-compile
java -ea -cp target/classes:target/test-classes com.holovin.npclife.EconomySmokeTest
java -ea -cp target/classes:target/test-classes com.holovin.npclife.EconomyIntegrationSmokeTest
mvn -o -Dexec.executable=java -Dexec.classpathScope=test '-Dexec.args=-ea -cp %classpath com.holovin.npclife.EconomyDatabaseSmokeTest' exec:exec
```

## Limitations

There is no property ownership, asset inventory, credit score, loan interest, legal enforcement, salary negotiation, job loss or debtor conversation dialogue yet. Expense intervals and lending heuristics are simplified game rules, not real economic forecasts.
