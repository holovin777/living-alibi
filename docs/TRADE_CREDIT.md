# Supplier credit and insolvency

Scheduled domestic factory purchases can use a three-game-day invoice instead
of immediate payment if the buyer has low cash, no wage arrears, no missed bank
installments and no outstanding supplier invoice, and the seller can afford
the exposure. Credit is limited to one input per batch and one outstanding
invoice per factory, up to EUR 6. Imported goods remain prepaid.
The actual stock is consumed on delivery. No cash is created.

At 06:30 wage arrears are settled first, then due supplier claims are paid
from cash above the existing EUR 40 working reserve. Partial payments are
supported. The same invoice is both the buyer's liability and seller's asset.
Three days of unresolved insolvency (including due unpaid supplier bills
or repeated missed bank installments) can suspend production and dismiss staff
through the existing solvency path. The bank can fund overdue supplier claims
through its existing reserve-protected rescue loans. Acquisitions account for
supplier liabilities and do not erase them. Dividends are blocked while supplier
debt exists.

The office workstation displays payables, receivables, the latest credit
invoice for each input, its due day, outstanding amount and payment status.
The existing SQLite stock map stores the records. No changes to save schema.
The invoice history is bounded, there is no court liquidation, and ownership
acquisition remains the existing player mechanism. NPC incorporation and
NPC-to-NPC mergers need a separate owner registry and company lifecycle.
