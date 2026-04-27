package tech.aiflowy.wiki.agentsflex;

import com.agentsflex.wiki.WikiProvider;
import com.agentsflex.wiki.WikiTool;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import tech.aiflowy.wiki.entity.Wiki;
import tech.aiflowy.wiki.service.WikiService;

import java.util.ArrayList;
import java.util.List;

@Component
public class AIFlowyWikiProvider implements WikiProvider {

    @Resource
    private WikiService wikiService;

    @Override
    public com.agentsflex.wiki.Wiki getWiki(String s) {

        Wiki record = wikiService.getById(s);
        if (record != null) {
            QueryWrapper w = QueryWrapper.create();
            w.eq(Wiki::getParentId, record.getId());
            List<Wiki> children = wikiService.list(w);
            com.agentsflex.wiki.Wiki afWiki = new com.agentsflex.wiki.Wiki();
            afWiki.setPath(record.getId().toString());
            afWiki.setTitle(record.getTitle());
            afWiki.setSummary(record.getDescription());
            if (children == null || children.isEmpty()) {
                afWiki.setContent(record.getContent());
            } else {
                List<com.agentsflex.wiki.Wiki> wikis = makeAgentsFlexWikis(children);
                String content = WikiTool.buildWikisXml(wikis);
                afWiki.setContent(content);
            }
            return afWiki;
        }
        return null;

    }

    public List<com.agentsflex.wiki.Wiki> makeAgentsFlexWikis(List<Wiki> aiflowyWikis) {
        List<com.agentsflex.wiki.Wiki> wikis = new ArrayList<>();
        for (Wiki child : aiflowyWikis) {
            com.agentsflex.wiki.Wiki afChildWiki = new com.agentsflex.wiki.Wiki();
            afChildWiki.setPath(child.getId().toString());
            afChildWiki.setTitle(child.getTitle());
            afChildWiki.setSummary(child.getDescription());
            afChildWiki.setContent(child.getContent());
            wikis.add(afChildWiki);
        }
        return wikis;
    }
}
