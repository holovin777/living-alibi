# Organized crime — fictional faction (initial engine implementation)

One hidden group of up to three existing adult residents is established in a town with at least 24 residents. Its members keep ordinary occupations, homes, needs and schedules. Police employees are excluded. No new named real-world organizations or residents are introduced.

Group affiliation modestly increases the existing fictional motive score. All normal incident opportunity checks, cooldowns, physical wallet custody, witness evidence and player investigation rules remain in force. No new crimes are forced on a schedule. Affiliation never counts as evidence of individual guilt. Public archive notices and the newspaper report incidents and resolutions without publishing secret member identities.

Resolved linked cases remove the implicated member from active faction participation on the next daily update. Leadership passes to a remaining member when necessary. This is faction attrition, not an NPC arrest or court sentence. Existing player custody is unchanged. There is no player membership, recruitment dialogue, faction-specific combat, or new 3D location yet.

Private state uses the existing persisted industry stock key/value table; old saves are not reset. The feature requires a populated town and a daily simulation update at 10:00.

Regression test: `mvn -q test-compile` then `java -ea -cp target/classes:target/test-classes com.holovin.npclife.MafiaSmokeTest`.
