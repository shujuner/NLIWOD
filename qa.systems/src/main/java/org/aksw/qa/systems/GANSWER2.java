package org.aksw.qa.systems;

public class GANSWER2 extends Gen_HTTP_QA_Sys {
	
	private static final String URL = "http://ganswer.gstore-pku.com/api/qald.jsp";
	
	public GANSWER2() {
		super(URL, "gAnswer2", false, false);
	}
	
	public GANSWER2(String url) {
		super(url, "gAnswer2", false, false);
	}
}