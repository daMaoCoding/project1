package com.xinbo.fundstransfer.domain.enums;

/**
 * Created by Owner on 2019/8/26.
 */
public interface AgentEnums {

	enum IsAgent {
		/**
		 * 是代理
		 */
		YES(true),

		/**
		 * 非代理
		 */
		NO(false);

		boolean agent;

		IsAgent(boolean agent) {
			this.agent = agent;
		}

		public boolean isAgent() {
			return agent;
		}
	}

	enum AgentType {
		/**
		 * 是代理
		 */
		Merge(1),

		/**
		 * 非代理
		 */
		Separate(2);

		int type;

		AgentType(int type) {
			this.type = type;
		}

		public int getType() {
			return type;
		}
	}
}
