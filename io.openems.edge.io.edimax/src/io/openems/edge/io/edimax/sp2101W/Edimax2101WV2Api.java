package io.openems.edge.io.edimax.sp2101W;

import java.io.StringReader;
import java.net.URI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.client.util.DigestAuthentication;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

/**
 * Implements the local Edimax 2101W V2 REST Api.
 * 
 * See https://github.com/camueller/SmartApplianceEnabler/blob/master/doc/EdimaxSP2101W_DE.md
 * 
 * For getting the password for the Device with firmware >= 3.00c see
 * https://discourse.nodered.org/t/searching-for-help-to-read-status-of-edimax-smartplug/15789/5
 * 
 */
public class Edimax2101WV2Api {

	private final String baseUrl;
	private final String realm;
	private final String user;
	private final String pass;

	public Edimax2101WV2Api(String ip, String Port, String Realm, String User, String Password) {
		this.baseUrl = "http://" + ip + ":" + Port;
		this.realm = Realm;
		this.user = User;
		this.pass = Password;
	}

	/**
	 * Gets the status of the device.
	 * 
	 * See https://github.com/camueller/SmartApplianceEnabler/blob/master/doc/EdimaxSP2101W_DE.md
	 * 
	 * @return the status as JsonObject according to doc
	 * @throws OpenemsNamedException 
	 */
	public JsonObject getStatus() throws OpenemsNamedException {
		Document doc = this.sendPostRequest("/smartplug.cgi", 
				"<?xml version=\"1.0\" encoding=\"UTF8\"?>" + 
				"<SMARTPLUG id=\"edimax\">" + 
				"<CMD id=\"get\">" + 
				"<NOW_POWER>" + 
				"<Device.System.Power.NowCurrent>" + 
				"</Device.System.Power.NowCurrent>" + 
				"<Device.System.Power.NowPower>" + 
				"</Device.System.Power.NowPower>" + 
				"</NOW_POWER>" + 
				"<Device.System.Power.State>" + 
				"</Device.System.Power.State>" + 
				"</CMD>" + 
				"</SMARTPLUG>");
		
		// status
		NodeList nodes = doc.getElementsByTagName("Device.System.Power.State");
		String Wert = nodes.item(0).getTextContent();
		Boolean bWert;
		if (Wert.equals("ON")) {
			bWert = true;
		}
		else bWert = false;
		
		JsonElement element = JsonUtils.getAsJsonElement(bWert);
		JsonObject jsonObject = new JsonObject();
		jsonObject.add("ison", element);
		
		// Current
		NodeList nodesC = doc.getElementsByTagName("Device.System.Power.NowCurrent");
		double current = Double.parseDouble(nodesC.item(0).getTextContent()) * 1000;		
		JsonElement elementC = JsonUtils.getAsJsonElement((int)current);
		jsonObject.add("current", elementC);
		
		// Power
		NodeList nodesP = doc.getElementsByTagName("Device.System.Power.NowPower");
		double power = Double.parseDouble(nodesP.item(0).getTextContent());		
		JsonElement elementP = JsonUtils.getAsJsonElement((int)power);
		jsonObject.add("power", elementP);
		
		return jsonObject;
	}


	/**
	 * Turns the relay with the given index on or off.
	 * 
	 * See https://github.com/camueller/SmartApplianceEnabler/blob/master/doc/EdimaxSP2101W_DE.md
	 * 
	 * @param index the index of the relay
	 * @param value true to turn on; false to turn off
	 * @throws OpenemsNamedException on error
	 */
	public void setRelayTurn(boolean value) throws OpenemsNamedException {
		if (value) {
			this.sendPostRequest("/smartplug.cgi", "<?xml version=\"1.0\" encoding=\"UTF8\"?><SMARTPLUG id=\"edimax\"><CMD id=\"setup\"><Device.System.Power.State>ON</Device.System.Power.State></CMD></SMARTPLUG>");
		}
		else {
			this.sendPostRequest("/smartplug.cgi", "<?xml version=\"1.0\" encoding=\"UTF8\"?><SMARTPLUG id=\"edimax\"><CMD id=\"setup\"><Device.System.Power.State>OFF</Device.System.Power.State></CMD></SMARTPLUG>");
		}
	}
	

	/**
	 * Sends a POST request to the Edimax API.
	 * 
	 * @param endpoint the REST Api endpoint
	 * @return a JsonObject or JsonArray
	 * @throws OpenemsNamedException on error
	 */
	private Document sendPostRequest(String uri, String data) throws OpenemsException {		
		try {				
			// Instantiate HttpClient
			HttpClient httpClient = new HttpClient();
			httpClient.start();
			
			// xml Data as Bytes
			byte[] xmlData = data.getBytes();
			
			URI url = new URI(this.baseUrl+uri);
			
			// Add authentication credentials
			AuthenticationStore auth = httpClient.getAuthenticationStore();
			auth.addAuthentication(new DigestAuthentication(url, this.realm, this.user, this.pass));
			
			Request request = httpClient.POST(url);
			request.content(new BytesContentProvider(xmlData), "application/xml");			
			ContentResponse response = request.send();

			int status = response.getStatus();
			
			if (status == 200) {
				String xmlResponse = response.getContentAsString();				
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document document = builder.parse(new InputSource(new StringReader(xmlResponse)));
				httpClient.stop();
				return document;
			} else {
				throw new OpenemsException(
						"Error while reading from Edimax API. Response code: " + status + ". " + response.toString());
			}   			
            
		} catch (Exception e) {
			throw new OpenemsException(
					"Unable to read from Edimax API. " + e.getClass().getSimpleName() + ": " + e.getMessage());
		} 
	}

}