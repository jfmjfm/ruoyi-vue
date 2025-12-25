/**
 * case_dir 数据格式迁移工具
 * 
 * 用途：将旧格式的 case_dir 数据转换为新的 JSON 格式
 * 
 * 使用方法：
 * node migrate_case_dir.js
 */

/**
 * 解析旧格式的 case_dir
 * @param {string} oldCaseDir - 旧格式的 case_dir 字符串
 * @returns {object|null} 解析后的对象
 */
function parseOldFormat(oldCaseDir) {
  if (!oldCaseDir || typeof oldCaseDir !== 'string') {
    return null;
  }
  
  // 移除开头的 @ 符号（如果存在）
  let cleanDir = oldCaseDir.trim();
  if (cleanDir.startsWith('@')) {
    cleanDir = cleanDir.substring(1);
  }
  
  // 使用 ||| 分隔符分割
  const parts = cleanDir.split('|||');
  
  if (parts.length < 2) {
    console.error('旧格式数据格式错误：缺少配置部分');
    return null;
  }
  
  try {
    const url = parts[0].trim();
    const config = JSON.parse(parts[1].trim());
    
    return {
      url: url,
      config: config
    };
  } catch (error) {
    console.error('解析旧格式配置失败:', error);
    return null;
  }
}

/**
 * 从 URL 中提取参数
 * @param {string} url - WMS URL
 * @returns {object} URL 参数对象
 */
function parseUrlParams(url) {
  const params = {
    base_url: '',
    service: 'WMS',
    version: '1.1.0',
    request: 'GetMap',
    layers: '',
    styles: '',
    srs: 'EPSG:4326',
    bbox: '',
    width: 1024,
    height: 768,
    format: 'image/png',
    transparent: 'true',
    bgcolor: '0x000000',
    viewparams: {}
  };
  
  try {
    const urlObj = new URL(url);
    params.base_url = `${urlObj.protocol}//${urlObj.host}${urlObj.pathname}`;
    
    // 提取 URL 参数
    urlObj.searchParams.forEach((value, key) => {
      const lowerKey = key.toLowerCase();
      
      if (lowerKey === 'layers') {
        params.layers = value;
      } else if (lowerKey === 'styles') {
        params.styles = value;
      } else if (lowerKey === 'srs' || lowerKey === 'crs') {
        params.srs = value;
      } else if (lowerKey === 'bbox') {
        params.bbox = value;
      } else if (lowerKey === 'width') {
        params.width = parseInt(value);
      } else if (lowerKey === 'height') {
        params.height = parseInt(value);
      } else if (lowerKey === 'format') {
        params.format = value;
      } else if (lowerKey === 'transparent') {
        params.transparent = value;
      } else if (lowerKey === 'version') {
        params.version = value;
      } else if (lowerKey === 'viewparams') {
        // 解析 viewparams
        const vpParts = value.split(';');
        vpParts.forEach(part => {
          const [k, v] = part.split(':');
          if (k && v) {
            params.viewparams[k.trim()] = v.trim();
          }
        });
      }
    });
    
  } catch (error) {
    console.error('解析 URL 失败:', error);
  }
  
  return params;
}

/**
 * 将旧格式的 scales 转换为新格式的 scales_info
 * @param {array} oldScales - 旧格式的 scales 数组
 * @returns {array} 新格式的 scales_info 数组
 */
function convertScales(oldScales) {
  if (!Array.isArray(oldScales)) {
    return [];
  }
  
  return oldScales.map(scale => {
    // 新格式需要的字段
    const newScale = {
      time: scale.time || [],
      layers: scale.layers || '',
      styles: scale.styles || scale.style || '',
      levels: scale.levels || [],
      scale: scale.scale || 'yearly',
      duration: scale.duration || 10
    };
    
    return newScale;
  });
}

/**
 * 将旧格式转换为新格式
 * @param {string} oldCaseDir - 旧格式的 case_dir 字符串
 * @returns {object|null} 新格式的 JSON 对象
 */
function migrateToNewFormat(oldCaseDir) {
  const parsed = parseOldFormat(oldCaseDir);
  
  if (!parsed) {
    console.error('无法解析旧格式数据');
    return null;
  }
  
  // 提取 URL 参数
  const urlParams = parseUrlParams(parsed.url);
  
  // 确保 viewparams 包含必要的字段
  if (!urlParams.viewparams.sim_time && parsed.config.time_start) {
    urlParams.viewparams.sim_time = parsed.config.time_start;
  }
  if (!urlParams.viewparams.scale) {
    urlParams.viewparams.scale = 'yearly';
  }
  
  // 转换 scales
  const scalesInfo = convertScales(parsed.config.scales || []);
  
  // 如果 scalesInfo 为空，从配置中创建默认的 scale
  if (scalesInfo.length === 0 && parsed.config.time_start && parsed.config.time_end) {
    scalesInfo.push({
      time: [parsed.config.time_start, parsed.config.time_end],
      layers: urlParams.layers.replace('repa:', ''),
      styles: urlParams.styles.replace('repa:', ''),
      levels: [],
      scale: parsed.config.units === 'month' ? 'monthly' : 'yearly',
      duration: parsed.config.duration || 10
    });
  }
  
  // 构建新格式
  const newFormat = {
    url_info: urlParams,
    scales_info: scalesInfo
  };
  
  return newFormat;
}

/**
 * 示例：迁移多个 case_dir 数据
 */
function migrateExamples() {
  // 示例1：基本格式
  const example1 = `http://172.16.124.1:31490/geoserver/repa/wms?service=WMS&version=1.1.0&request=GetMap&layers=repa:usle_swat&bbox=118.41667877737004,44.452917782192465,119.14330242199273,45.23305354431723&width=768&height=768&srs=EPSG:4326&styles=&format=image%2Fpng&transparent=true&viewparams=sim_time:2009-01-01;scale:yearly|||{"duration":10,"units":"year","time_start":"2009-01-01","time_end":"2018-12-01","scales":[{"scale":"yearly","style":"usle_swat_yearly","duration":10,"time":["2009-01-01","2018-12-01"],"levels":[0.06,0.186,0.354,0.562,0.789,1.028,1.244,1.493,1.775,1.9325]},{"scale":"monthly","style":"usle_swat_monthly","duration":120,"time":["2009-01-01","2018-12-01"],"levels":[0.906,3.486,7.585,14.008,23.118,38.938,57.002,77.798,97.52,145.099]}]}`;
  
  console.log('=== 示例1：迁移基本格式 ===\n');
  const migrated1 = migrateToNewFormat(example1);
  
  if (migrated1) {
    console.log('迁移成功！');
    console.log('\n新格式（JSON）：');
    console.log(JSON.stringify(migrated1, null, 2));
    console.log('\n新格式（字符串，用于存入数据库）：');
    console.log(JSON.stringify(migrated1));
  }
  
  console.log('\n=====================================\n');
}

// 运行示例
if (require.main === module) {
  migrateExamples();
  
  console.log('提示：');
  console.log('1. 复制上面输出的新格式字符串');
  console.log('2. 更新数据库中的 case_dir 字段');
  console.log('3. 或者使用 SQL 批量更新：');
  console.log('   UPDATE project_service_case SET case_dir = \'新格式字符串\' WHERE id = ?;');
}

// 导出函数供其他脚本使用
module.exports = {
  parseOldFormat,
  parseUrlParams,
  convertScales,
  migrateToNewFormat
};

