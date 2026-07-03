package tech.aiflowy.codegen;

import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.mybatisflex.codegen.dialect.JdbcTypeMapping;
import com.zaxxer.hikari.HikariDataSource;

public class DatacenterModuleGen {
    public static void main(String[] args) {
        //配置数据源
        HikariDataSource dataSource = new HikariDataSource();

        //注意：url 需添加上 useInformationSchema=true 才能正常获取表的注释
        dataSource.setJdbcUrl("jdbc:mysql://192.168.2.10:3306/aiflowy-v2?useInformationSchema=true&characterEncoding=utf-8&rewriteBatchedStatements=true");
        dataSource.setUsername("root");
        dataSource.setPassword("123456");

        // 设置时间类型为 Date
        JdbcTypeMapping.registerDateTypes();


        //生成 framework-modules/aiflowy-module-datacenter 下的代码
        GlobalConfig globalConfig = createGlobalConfig();
        Generator moduleGenerator = new Generator(dataSource, globalConfig);
        moduleGenerator.generate();

    }



    public static GlobalConfig createGlobalConfig() {

        String optionsColumns = "options,vector_store_options,llm_options";

        //创建配置内容
        GlobalConfig globalConfig = Util.createBaseConfig(optionsColumns);
        globalConfig.setBasePackage("tech.aiflowy.datacenter");


        globalConfig.setGenerateTable("tb_datacenter_table", "tb_datacenter_table_field");
        String sourceDir = System.getProperty("user.dir") + "/aiflowy-modules/aiflowy-module-datacenter/src/main/java";
        globalConfig.setSourceDir(sourceDir);

        return globalConfig;
    }




}
