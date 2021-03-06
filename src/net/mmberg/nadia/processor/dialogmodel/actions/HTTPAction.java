package net.mmberg.nadia.processor.dialogmodel.actions;

import java.io.StringReader;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import net.mmberg.nadia.dialogmodel.definition.actions.HTTPActionModel;
import net.mmberg.nadia.processor.NadiaProcessor;
import net.mmberg.nadia.processor.dialogmodel.Frame;

@XmlRootElement
public class HTTPAction extends HTTPActionModel {

	private HttpClient client;
	private final static Logger logger = NadiaProcessor.getLogger();
	
	public HTTPAction(){
		super();
		init();
	}
	
	public HTTPAction(String template){
		super(template);
		init();
	}
	
	private void init(){
		SslContextFactory sslContextFactory = new SslContextFactory();
    	client = new HttpClient(sslContextFactory);
    	try {
			client.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public HashMap<String, String> execute(Frame frame) {
		
		 	/*
		 	 * WikiAPI: http://www.mediawiki.org/wiki/Extension:MobileFrontend
		 	 * e.g.,
		 	 * url="http://en.wikipedia.org/w/api.php?format=xml&action=query&prop=extracts&explaintext&exsentences=3&titles=Edinburgh";
		 	 * xpath="//extract";
		 	 * 
		 	 * or
		 	 * 
		 	 * curl --data "state=on" http://mmt.et.hs-wismar.de:8080/Lightbulb/Lightbulb
		 	 * 
		 	 */
		
			String replaced_url=replaceSlotMarkers(url, frame);
			String replaced_params=replaceSlotMarkers(params, frame);
			String[] params_arr = replaced_params.split("&");
			
			String result="Sorry, that did not work. ";
	        try{
	        		        	 
	        	ContentResponse response;
	        	Request request;
        		
	        	request = client.newRequest(replaced_url);

	        	//choose method
	        	if(method.toLowerCase().equals("get")){
	        		request.method(HttpMethod.GET);
	        	}
	        	else{
	        		request.method(HttpMethod.POST);
	        	}
	        	
	        	//process parameters
	        	String[] key_value;
	        	for(String paramPair : params_arr){
	        		key_value=paramPair.split("=");
	        		if(key_value.length>1) request.param(key_value[0],key_value[1]);
	        		else request.param(key_value[0], "");
	        	}       	
	        	
	        	logger.info("requesting: "+request.getURI()+", "+request.getParams().toString());
	        	response = request.send();
	        	logger.info("HTTP status: "+response.getStatus());
	        	
	        	String xml = response.getContentAsString();
	        	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		        DocumentBuilder builder = factory.newDocumentBuilder();
	        	Document doc = builder.parse(new InputSource(new StringReader(xml)));
	        	 
		        XPathFactory xPathfactory = XPathFactory.newInstance();
		        XPath xPath = xPathfactory.newXPath();
		        XPathExpression expr = xPath.compile(xpath);
		        result = (String) expr.evaluate(doc, XPathConstants.STRING);
		        
		        //Postprocessing
		        result = result.replaceAll("\\s\\(.*?\\)", ""); //remove content in brackets
		        result = result.replaceAll("\\s\\[.*?\\]", "");
	        }
	        catch(Exception ex){
	        	ex.printStackTrace();
	        }
	        executionResults.put("result", result);
			return executionResults;
	}

}
