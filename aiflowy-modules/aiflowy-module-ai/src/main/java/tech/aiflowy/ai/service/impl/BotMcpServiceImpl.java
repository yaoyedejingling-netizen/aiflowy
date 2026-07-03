package tech.aiflowy.ai.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import tech.aiflowy.ai.entity.BotMcp;
import tech.aiflowy.ai.mapper.BotMcpMapper;
import tech.aiflowy.ai.service.BotMcpService;
import org.springframework.stereotype.Service;
import tech.aiflowy.common.audio.socket.AudioSocketHandler;
import tech.aiflowy.log.annotation.LogRecord;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *  服务层实现。
 *
 * @author wangGangQiang
 * @since 2026-01-05
 */
@Service
public class BotMcpServiceImpl extends ServiceImpl<BotMcpMapper, BotMcp>  implements BotMcpService{

    private final static Logger log = LoggerFactory.getLogger(BotMcpServiceImpl.class);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBotMcpToolIds(BigInteger botId, List<Map<String, List<List<String>>>> mcpSelectedData) {
        // 删除原来绑定的mcp
        this.remove(QueryWrapper.create().eq(BotMcp::getBotId, botId));
        if (mcpSelectedData == null || mcpSelectedData.isEmpty()) {
            return;
        }
        List<BotMcp> list = new ArrayList<>();
        for (Map<String, List<List<String>>> mcpItem : mcpSelectedData) {
            if (mcpItem == null) {
                continue;
            }
            for (Map.Entry<String, List<List<String>>> entry : mcpItem.entrySet()) {
                String mcpId = entry.getKey();
                List<List<String>> toolList = entry.getValue();
                if (toolList == null || toolList.isEmpty()) {
                    continue;
                }
                //复用mcpId
                BigInteger mcpIdValue = new BigInteger(mcpId);
                for (List<String> toolInfo : toolList) {
                    if(CollectionUtil.isNotEmpty(toolInfo)){
                        BotMcp botMcp = new BotMcp();
                        botMcp.setBotId(botId);
                        botMcp.setMcpId(mcpIdValue);
                        botMcp.setMcpToolName(toolInfo.get(0));
                        botMcp.setMcpToolDescription(toolInfo.get(1));
                        list.add(botMcp);
                        log.info("工具名称：" + toolInfo.get(0) + "，描述：" + toolInfo.get(1));
                    }
                }
            }
        }
        if (!list.isEmpty()) {
            this.saveBatch(list);
        }
    }
}
