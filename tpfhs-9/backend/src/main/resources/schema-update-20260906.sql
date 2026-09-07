-- Restore the default carbon accounting template for databases initialized
-- before schema.sql started assigning the root node to a template.
START TRANSACTION;

INSERT INTO emission_template
    (name, description, created_by, updated_by, version, enabled, template_type)
SELECT
    '朔黄铁路碳排放核算模板',
    '系统初始化的朔黄铁路碳排放核算模板，可继续维护节点、采集点、因子和调度参数。',
    1, 1, 1, FALSE, 2
WHERE NOT EXISTS (
    SELECT 1 FROM emission_template WHERE name = '朔黄铁路碳排放核算模板'
);

SET @default_template_id = (
    SELECT id FROM emission_template
    WHERE name = '朔黄铁路碳排放核算模板'
    ORDER BY id LIMIT 1
);

UPDATE emission_node
SET template_id = @default_template_id,
    updated_by = COALESCE(updated_by, 1)
WHERE parent_id IS NULL
  AND template_id IS NULL
  AND name = '碳排放核算'
ORDER BY id
LIMIT 1;

INSERT INTO emission_node
    (name, type_id, parent_id, template_id, sort_order, created_by, updated_by)
SELECT
    '碳排放核算', nt.id, NULL, @default_template_id, 0, 1, 1
FROM emission_node_type nt
WHERE nt.type_name = 'root'
  AND NOT EXISTS (
      SELECT 1 FROM emission_node
      WHERE template_id = @default_template_id AND parent_id IS NULL
  )
LIMIT 1;

COMMIT;
