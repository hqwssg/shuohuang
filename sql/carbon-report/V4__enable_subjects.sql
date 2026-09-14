-- Enable after live BRANCH node IDs are confirmed. Do not guess.
-- Replace the NULLs, then set enabled='1'.

-- UPDATE report_dept_subject
--    SET subject_node_id = /* emission_node.id where node_category=BRANCH for 肃宁 */,
--        enabled = '1',
--        update_by = 'admin',
--        update_time = NOW()
--  WHERE energy_allocation = 'suring';

-- UPDATE report_dept_subject
--    SET subject_node_id = /* BRANCH node for 原平 */,
--        enabled = '1',
--        update_by = 'admin',
--        update_time = NOW()
--  WHERE energy_allocation = 'yuanping';

-- UPDATE report_dept_subject
--    SET subject_node_id = /* BRANCH node for 机辆 */,
--        enabled = '1',
--        update_by = 'admin',
--        update_time = NOW()
--  WHERE energy_allocation = 'jilong';
