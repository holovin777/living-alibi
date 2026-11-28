# Menu stability
Explicit engineering, software, campus, medical and cafe requests take
precedence over nearby automatic vehicle menus. The classic client has
fixed close buttons above the action and travel scroll areas. Escape
closes active UI even when an input has focus. Closing restores capture
when no other menu remains, and clears stale contextual menu flags.
The request lock preserves each button's original disabled state rather
than enabling unavailable actions on completion. Rebuilt travel buttons
record their actual availability separately from transient locks.
Geographic client avoids toggling menus while typing into LineEdit or
TextEdit fields and releases keyboard focus on close.
The existing scene test now checks real button signal callbacks and
enabled/disabled state across a request. Java remains authoritative.
