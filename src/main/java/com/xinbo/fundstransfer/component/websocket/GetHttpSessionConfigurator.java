package com.xinbo.fundstransfer.component.websocket;

import com.xinbo.fundstransfer.AppConstants;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

/**
 * Created by eden on 2017/10/4.
 */
public class GetHttpSessionConfigurator extends ServerEndpointConfig.Configurator {

	@Override
	public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
		try {
			for (String kv : request.getHeaders().get("cookie").get(0).split(";")) {
				if (kv.contains(AppConstants.JSESSIONID)) {
					sec.getUserProperties().put(AppConstants.JSESSIONID, kv.split("=")[1]);
					break;
				}
			}
		} catch (Exception e) {
		}
	}
}
