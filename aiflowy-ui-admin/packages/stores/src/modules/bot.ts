import { acceptHMRUpdate, defineStore } from 'pinia';

// 定义预设问题类型
export type PresetQuestionsType = {
  description: string;
  key: string;
};

// 仅包含预设问题的状态接口
interface BotStoreState {
  /** 预设问题列表 */
  presetQuestions: Map<string, PresetQuestionsType[]>;
}

/**
 * @zh_CN 预设问题相关 store
 */
export const useBotStore = defineStore('bot-preset', {
  state: (): BotStoreState => ({
    // 初始化空 Map
    presetQuestions: new Map(),
  }),
  actions: {
    getPresetQuestions(botId?: string): PresetQuestionsType[] {
      if (botId) {
        return this.presetQuestions.get(botId) ?? [];
      }
      return [];
    },
    setPresetQuestions(botId: string, questions: PresetQuestionsType[]) {
      this.presetQuestions.set(botId, questions);
    },
  },
});

// 解决热更新问题
if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useBotStore, import.meta.hot));
}
