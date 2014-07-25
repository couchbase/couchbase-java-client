package com.couchbase.client.java;

import com.couchbase.client.core.message.ResponseStatus;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.LongDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.util.ClusterDependentTest;
import com.couchbase.client.java.util.SSLTestSupportFunc;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;
import rx.Observable;
import rx.functions.Func1;
import rx.observables.BlockingObservable;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.couchbase.client.core.env.Environment;
import com.couchbase.client.core.endpoint.SSLException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;



public class SSLTest{
	
	ClusterDependentTest cluster = new ClusterDependentTest();	 
	SSLTestSupportFunc ssl_test = new SSLTestSupportFunc();
	  
	Config conf = ConfigFactory.load();
	String keyStoreFile = conf.getString("com.couchbase.client.bootstrap.sslKeystoreFile");
	File client_cacert = new File(keyStoreFile);  	  
	
	
  	/**
  	* Attempt to make client certificate unavailable by renaming it
  	* @post Reconnect fail and no upsert ops after the certificate is renamed
  	* and returns an error if reconnect successfully
  	*
  	* @throws Exception
  	*/
	@Test
	public void ClientCertificateUnavailable(){
		File client_cacert_rename = new File(keyStoreFile.concat("_rename"));
		
		try{
			cluster.connect();
			ssl_test.shouldUpsertAndGet("Unavialble", "client", "cert available");    
			cluster.disconnect();
			
		    if(client_cacert_rename.exists()){
		    	throw new java.io.IOException(client_cacert_rename.getName() +" exists");
		    }
		    
		    //rename cacert to make it unavailable
		    if (!client_cacert.renameTo(client_cacert_rename)){
		    	System.err.println("Error rename " + client_cacert.getName());
		    }
		    
		    System.out.println("Reconnect with the Server");
		    cluster.connect();
			
			ssl_test.shouldUpsertAndGet("Unavailable", "client", "cert unavailable");
		    fail("Should have thrown an Exception because certificate is not available!");
		}
		catch (Exception e){
			e.printStackTrace();
		}
		finally{
			if (!client_cacert_rename.renameTo(client_cacert)){
				System.err.println("Error rename "+ client_cacert.getName()+ " back");
			}
			
		    try{
				cluster.disconnect();
		    }
		    catch (Exception e){
		    	 e.printStackTrace();
		    }
		}
	}
	
	
	/**
  	* Using a expired certificate to reconnect
  	* @post Reconnect fail and no upsert ops 
  	* returns an error if reconnect successfully
  	*
  	* @throws Exception
  	*/
	@Test
	public void ClientCertificateExpiry(){
		File client_cacert_copy= new File(keyStoreFile.concat("_cp"));
		
		try{
			cluster.connect();
			ssl_test.shouldUpsertAndGet("Expiry", "client", "cert valid");  
			cluster.disconnect();

			//make a copy of original cacert
            String SourceFile = client_cacert.getAbsolutePath();
            String DestinationFile = client_cacert_copy.getAbsolutePath();            
            ssl_test.copyCert(SourceFile, DestinationFile);
           	
            //delete original cacert
			if(!client_cacert.delete()){
    			System.out.println("Delete" + client_cacert.getName() +" failed.");
    		}
			
			//make new cacert with expired date
			Process p;
		    p = Runtime.getRuntime().exec("keytool -genkey -noprompt -keypass couchbase -storepass couchbase -keystore " +client_cacert.getAbsolutePath()+ " -alias ssl -validity 1 -dname CN=Unknown -startdate 1970/01/01");
		    if (p.waitFor() == 0){
		    	System.out.println("expired cert created") ;
		    }
		    else{
				System.err.println("generate a expired certificate fail");
		    }
		    
		    System.out.println("Reconnect with the Server");
		    cluster.connect();
			
			ssl_test.shouldUpsertAndGet("Expiry", "client", "cert invalid");
		    fail("Should have thrown an Exception because certificate expired!");
		}	
		catch (Exception e){
			e.printStackTrace();
		}
		finally{
			try{				
				if(!client_cacert.delete()){
	    			System.err.println("Delete" + client_cacert.getName() +" failed.");
	    		}
				if (!client_cacert_copy.renameTo(client_cacert)){
					System.err.println("Error rename cacert back");
				}
				
				cluster.disconnect();
		    }
		    catch (Exception e){
		    	 e.printStackTrace();
		    }
		}
	}
	
	
	/**
  	* Using a randomly generated certificate to reconnect
  	* @post Reconnect fail and no upsert ops 
  	* returns an error if reconnect successfully
  	*
  	* @throws Exception
  	*/
	@Test
	public void ClientCertificateRefresh(){ 
		File client_cacert_copy= new File(keyStoreFile.concat("_cp"));
		
		try{
			cluster.connect();
			ssl_test.shouldUpsertAndGet("Refresh", "client", "valid cert");    
			cluster.disconnect();
					 
			//make a copy of the original cacert
			String SourceFile = client_cacert.getAbsolutePath();
            String DestinationFile = client_cacert_copy.getAbsolutePath();            
            ssl_test.copyCert(SourceFile, DestinationFile);
		    
            //delete the original cacert
			if(!client_cacert.delete()){
				System.err.println("Delete" + client_cacert.getName() +" failed.");
			}			
            
		    //create a random keystore
		    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		    char[] password = "couchbase".toCharArray();
		    ks.load(null, password);
		    FileOutputStream fos = new FileOutputStream(client_cacert.getAbsolutePath());
		    ks.store(fos, password);
		    fos.close();
		    System.setProperty("javax.net.ssl.trustStore", client_cacert.getAbsolutePath());
		    
		    System.out.println("Reconnect with the Server");
		    cluster.connect();
			
			ssl_test.shouldUpsertAndGet("Refresh", "client", "random cert");
		    fail("Should have thrown an Exception because certificate is randomly generate!");
		    
		}
		catch (Exception e){
			e.printStackTrace();
		}
		finally{
		    try{
				if(!client_cacert.delete()){
	    			System.err.println("Delete" + client_cacert.getName() +" failed.");
	    		}
				if (!client_cacert_copy.renameTo(client_cacert)){
					System.err.println("Error rename cacert back");
				}
				
				cluster.disconnect();
		    }
		    catch (Exception e){
		    	 e.printStackTrace();
		    }
		}
	  
	}

	
	/**
  	* Generate a new SSL certificate on server then reconnect
  	* @post Reconnect fail and no upsert ops 
  	* returns an error if reconnect successfully
  	*
  	* @throws Exception
  	*/
	@Test
	public void ServerRegenerateCertificate(){	
		String server_username = conf.getString("com.couchbase.client.bootstrap.server_username");
		String server_password = conf.getString("com.couchbase.client.bootstrap.server_password");
		String serverCertPath = conf.getString("com.couchbase.client.bootstrap.serverCertPath");
		String sslKeystorePassword = conf.getString("com.couchbase.client.bootstrap.sslKeystorePassword");
		
		try{			
			Process p;
			StringBuffer output = new StringBuffer();
			String line = "";
			
	    	cluster.connect();
			ssl_test.shouldUpsertAndGet("Regenerate", "server", "original cert");    
			cluster.disconnect();
			
			p = Runtime.getRuntime().exec("curl -vvv -XPOST --user "+server_username+":"+server_password+" http://127.0.0.1:8091/controller/regenerateCertificate");
			if (p.waitFor() == 0){
				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				while ((line = reader.readLine())!= null) {
					output.append(line + "\n");
				}
				System.out.println("generate new server cert\n"+output.toString());
		    }
		    else{
				System.err.println("generate new server certificate fail");
			}

		    System.out.println("Reconnect with the Server");
		    cluster.connect();
			
			ssl_test.shouldUpsertAndGet("Regenerate", "server", "updated cert");
		    fail("Should have thrown an Exception because client certificate is outdated!");   
		}
		catch (Exception e){
			e.printStackTrace();
		}
		finally{
		    try{		    	
		    	//load the current keystore content
		    	File keystoreFile = new File(client_cacert.getAbsolutePath());
		    	FileInputStream is = new FileInputStream(keystoreFile);
		    	KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
		    	keystore.load(is, sslKeystorePassword.toCharArray());

		    	//import the ssl server certificate
		    	String alias = "ssl_integrationTest";
		    	char[] password = sslKeystorePassword.toCharArray();

		    	CertificateFactory cf = CertificateFactory.getInstance("X.509");
		    	InputStream certstream = ssl_test.fullStream (serverCertPath);
		    	Certificate certs =  cf.generateCertificate(certstream);

		    	// Add the certificate
		    	keystore.setCertificateEntry(alias, certs);

		    	// Save the new keystore contents
		    	FileOutputStream out = new FileOutputStream(keystoreFile);
		    	keystore.store(out, password);
		    	out.close();
	    	
				cluster.disconnect();
		    }
		    catch (Exception e){
		    	 e.printStackTrace();
		    }
		}
	}
  
}