# Industrial procurement

The existing factory network now creates daily purchase orders for its actual
inputs at 10:00, with a second planning opportunity at 13:00. An appointed
working director authorizes purchases for a delegated factory; otherwise a
working employee acts as purchasing representative. Orders require sufficient
cash for the quote, and don't transfer money or reserve shared stock yet.
Imported materials use the existing paid regional supply and fuel route.
Domestic input settlement occurs atomically with production after power, gas,
machine, inventory and total funds checks. Orders unfulfilled at production
cutoff are recorded as such; they do not create invoices or debts.

The order records include supplier, quantity, quote, day and status. The most
recent paid invoice per input is preserved even when a later order fails.
Careful buyers may negotiate a one-euro discount for a bulk domestic purchase
from a low-reputation producer. The two timber buyers alternate first access
to scarce stock, preserving the downstream production sequence. Other
suppliers remain fixed by the current production network.

Owned factories expose purchase orders and last paid invoices in the office
computer. All state is persisted in industryStocks. No separate warehouse,
money or parallel production path is introduced. External imports use the
existing abstract regional supplier. This is not yet a full supplier market,
negotiation conversation, invoice archive or credit-payment system.
