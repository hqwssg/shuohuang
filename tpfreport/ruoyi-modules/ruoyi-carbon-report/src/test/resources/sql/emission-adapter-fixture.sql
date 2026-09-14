-- Minimal two-department fixture used by EmissionSourceAdapterTest comments.
-- Department A (suring) has a successful calculation; department B (yuanping) has a failed one.
-- Cross-allocation rows must not leak.

-- dept A BRANCH node = 1001, energy_allocation = suring
-- dept B BRANCH node = 2001, energy_allocation = yuanping
-- calculation 11 status=2 (success) on node 1001, NG 2 10k_m3 emission 9
-- calculation 12 status=3 (failed) on node 2001
-- extra row on calc 11 with energy_allocation=yuanping must be ignored for suring
