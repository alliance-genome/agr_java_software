package org.alliancegenome.api.service;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MessageService {

	public void message() {
		System.out.println("Message Service:");
		
	}

}
