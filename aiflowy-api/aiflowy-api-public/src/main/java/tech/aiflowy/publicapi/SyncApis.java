package tech.aiflowy.publicapi;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.MybatisFlexBootstrap;
import com.mybatisflex.core.query.QueryWrapper;
import com.zaxxer.hikari.HikariDataSource;
import tech.aiflowy.system.entity.SysApiKeyResource;
import tech.aiflowy.system.mapper.SysApiKeyResourceMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 同步 SpringDoc 接口数据到数据库
 * 先启动项目，再运行此程序
 */
public class SyncApis {

    public static void main(String[] args) throws Exception {
        // 获取 SpringDoc 的 OpenAPI JSON 数据
        String apiDocsJson = HttpUtil.get("http://localhost:8080/v3/api-docs/public-api");
        System.out.println("获取到的 API 文档数据:");
        System.out.println(apiDocsJson);
        // 解析并同步到数据库
        syncApisToDatabase(apiDocsJson);
    }

    /**
     * 解析 OpenAPI JSON 数据并同步到数据库
     */
    public static void syncApisToDatabase(String apiDocsJson) throws Exception {
        // 解析 JSON
        JSONObject openApiDoc = JSONUtil.parseObj(apiDocsJson);

        // 提取 tags 信息（用于获取分组描述）
        Map<String, String> tagDescriptionMap = extractTagDescriptions(openApiDoc);

        // 提取 paths 信息
        List<SysApiKeyResource> apiResources = extractApiResources(openApiDoc, tagDescriptionMap);

        // 保存到数据库
        saveToDatabase(apiResources);
    }

    /**
     * 提取 tags 信息，建立 name -> description（优先）或 name 的映射
     */
    private static Map<String, String> extractTagDescriptions(JSONObject openApiDoc) {
        Map<String, String> tagMap = new HashMap<>();

        JSONArray tags = openApiDoc.getJSONArray("tags");
        if (tags != null) {
            for (int i = 0; i < tags.size(); i++) {
                JSONObject tag = tags.getJSONObject(i);
                String name = tag.getStr("name");
                String description = tag.getStr("description");

                // 优先使用 description，如果为空则使用 name
                String groupName = (description != null && !description.trim().isEmpty())
                        ? description.trim()
                        : name;

                tagMap.put(name, groupName);
            }
        }

        return tagMap;
    }

    /**
     * 提取所有 API 资源信息
     */
    private static List<SysApiKeyResource> extractApiResources(
            JSONObject openApiDoc,
            Map<String, String> tagDescriptionMap) {

        List<SysApiKeyResource> resources = new ArrayList<>();

        // 获取 paths 对象
        JSONObject paths = openApiDoc.getJSONObject("paths");
        if (paths == null) {
            return resources;
        }

        // 遍历所有路径
        for (String path : paths.keySet()) {
            JSONObject pathItem = paths.getJSONObject(path);

            // 遍历该路径下的所有 HTTP 方法
            for (String method : pathItem.keySet()) {
                // 跳过非操作字段（如 parameters）
                if ("parameters".equals(method)) {
                    continue;
                }

                JSONObject operation = pathItem.getJSONObject(method);
                if (operation == null) {
                    continue;
                }

                SysApiKeyResource resource = new SysApiKeyResource();

                // 设置请求接口路径
                resource.setRequestInterface(path);

                // 获取操作描述（summary 或 description）
                String title = operation.getStr("summary");
                if (title == null || title.trim().isEmpty()) {
                    title = operation.getStr("description");
                }
                resource.setTitle(title != null ? title.trim() : "");

                // 获取分组信息（从 tags 中取第一个 tag）
                JSONArray operationTags = operation.getJSONArray("tags");
                if (operationTags != null && !operationTags.isEmpty()) {
                    String firstTagName = operationTags.getStr(0);
                    // 从 tagDescriptionMap 获取分组名称（description 优先）
                    String groupName = tagDescriptionMap.getOrDefault(firstTagName, firstTagName);
                    resource.setGroupName(groupName);
                } else {
                    resource.setGroupName("");
                }

                resources.add(resource);

                // 打印解析结果
                System.out.println("--------------------------------");
                System.out.println("  HTTP方法: " + method.toUpperCase());
                System.out.println("  接口路径: " + path);
                System.out.println("  标题: " + resource.getTitle());
                System.out.println("  分组: " + resource.getGroupName());
            }
        }

        return resources;
    }

    /**
     * 保存 API 资源到数据库
     */
    private static void saveToDatabase(List<SysApiKeyResource> resources) throws Exception {
        try (HikariDataSource dataSource = new HikariDataSource()) {
            dataSource.setJdbcUrl("jdbc:mysql://192.168.2.10:3306/aiflowy-v2?useInformationSchema=true&characterEncoding=utf-8&rewriteBatchedStatements=true");
            dataSource.setUsername("root");
            dataSource.setPassword("123456");

            MybatisFlexBootstrap bootstrap = MybatisFlexBootstrap.getInstance();
            bootstrap.setDataSource(dataSource);
            bootstrap.addMapper(SysApiKeyResourceMapper.class);
            bootstrap.start();

            SysApiKeyResourceMapper mapper = bootstrap.getMapper(SysApiKeyResourceMapper.class);

            int inserted = 0;
            int updated = 0;

            for (SysApiKeyResource resource : resources) {
                // 查询是否已存在该接口
                QueryWrapper wrapper = QueryWrapper.create();
                wrapper.eq(SysApiKeyResource::getRequestInterface, resource.getRequestInterface());
                SysApiKeyResource existing = mapper.selectOneByQuery(wrapper);

                if (existing != null) {
                    // 更新现有记录
                    existing.setTitle(resource.getTitle());
                    existing.setGroupName(resource.getGroupName());
                    mapper.insertOrUpdate(existing);
                    updated++;
                } else {
                    // 插入新记录
                    mapper.insert(resource);
                    inserted++;
                }
            }

            System.out.println("=========================================");
            System.out.println("同步完成！");
            System.out.println("  新增: " + inserted + " 条");
            System.out.println("  更新: " + updated + " 条");
            System.out.println("  总计: " + resources.size() + " 条");
        }
    }
}
