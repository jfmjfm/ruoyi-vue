-- 删除触发器，如果存在
DROP TRIGGER IF EXISTS after_insert_project_region_service;

-- 重新创建触发器
DELIMITER //

CREATE TRIGGER after_insert_project_region_service
AFTER INSERT ON project_region_service
FOR EACH ROW
BEGIN
    DECLARE global_region_name VARCHAR(255);
    DECLARE global_service_name VARCHAR(255);

    SET global_region_name = (SELECT region_name FROM project_region WHERE id = NEW.region_id);
    SET global_service_name = (SELECT dict_label FROM sys_dict_data WHERE dict_value = NEW.service_type and dict_type = 'sys_service_type');

    INSERT INTO project_service_case (region_service_id, case_name, case_dir, is_default)
    VALUES (NEW.id, CONCAT(global_region_name, '-', global_service_name, '-default'), 'default path', 1);
END;

//

DELIMITER ;
