package tech.aiflowy.ai.config;

import com.agentsflex.engine.es.ElasticSearcher;
import com.agentsflex.search.engine.lucene.LuceneSearcher;
import com.agentsflex.search.engine.service.DocumentSearcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.math.BigInteger;

@Configuration
public class SearcherFactory {

    @Autowired
    private AiLuceneConfig luceneConfig;

    @Autowired
    private AiEsConfig aiEsConfig;

    @Value("${rag.searcher.type}")
    private String defaultSearcherType;

    @Value("${rag.searcher.lucene.indexDirPath}")
    private String baseIndexDirPath;

    @Bean
    public LuceneSearcher luceneSearcher() {
        return new LuceneSearcher(luceneConfig);
    }

    @Bean
    public ElasticSearcher elasticSearcher() {
        return new ElasticSearcher(aiEsConfig);
    }

    public DocumentSearcher getSearcher(String type) {
        return getSearcher(type, null);
    }

    public DocumentSearcher getSearcher(String type, BigInteger collectionId) {
        if (type == null || type.isEmpty()) {
            type = defaultSearcherType;
        }
        switch (type) {
            case "elasticSearch":
                return new ElasticSearcher(aiEsConfig);
            case "lucene":
            default:
                if (collectionId != null) {
                    // 动态构建索引路径
                    AiLuceneConfig dynamicConfig = new AiLuceneConfig();
                    dynamicConfig.setIndexDirPath(baseIndexDirPath + "/" + collectionId);
                    return new LuceneSearcher(dynamicConfig);
                }
                return new LuceneSearcher(luceneConfig);
        }
    }

    /**
     * 删除整个知识库的 Lucene 索引目录
     * @param collectionId 知识库ID
     * @return 是否删除成功
     */
    public boolean deleteCollectionIndex(BigInteger collectionId) {
        String indexPath = baseIndexDirPath + "/" + collectionId;
        File indexDir = new File(indexPath);
        if (indexDir.exists() && indexDir.isDirectory()) {
            return deleteDirectory(indexDir);
        }
        return true;
    }

    /**
     * 递归删除目录及其内容
     */
    private boolean deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        return directory.delete();
    }
}