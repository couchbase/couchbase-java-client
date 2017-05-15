package com.couchbase.client.java.util;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;

import rx.Observable;
import rx.functions.Func1;
import static org.junit.Assert.*;

/**
* class provides functions for SSL integrationTest
*
*/
public class SSLTestSupportFunc{
	
    private static final String bucketName = TestProperties.bucket();
    private static final String password = TestProperties.password();

    private static Cluster cluster;
    private static Bucket bucket;

    public void connect(CouchbaseEnvironment env) {
        cluster = CouchbaseCluster.create(env);
        bucket = cluster.openBucket(bucketName, password);
    }

    public void disconnect() throws InterruptedException {
        cluster.disconnect();
    }	
    
	
	/**
	* perform upsert and get ops
	*
	* @param id doccument id
	* @param key document key
	* @param value document value
	*/
	public void shouldUpsertAndGet(final String id, String key,String value) {
		
		JsonObject content = JsonObject.empty().put(key, value);
		final JsonDocument doc = JsonDocument.create(id, content);
		bucket.upsert(doc);
		JsonDocument response = bucket.get(id);

		assertEquals(content.getString(key), response.content().getString(key));
	}
	
	/**
	* make a copy of certificate file
	*
	* @param SourceFile original file path
	* @param DestinationFile file copy path
	*/
	public void copyCert(String SourceFile, String DestinationFile){
		try{
			FileInputStream fin = new FileInputStream(SourceFile);
			FileOutputStream fout = new FileOutputStream(DestinationFile);
			byte[] b = new byte[1024];
			int noOfBytes = 0;
			//read bytes from source file and write to destination file
			while( (noOfBytes = fin.read(b)) != -1 ){
				fout.write(b, 0, noOfBytes);
			}
			System.out.println("File copied!");
			fin.close();
			fout.close();
		}
		catch(FileNotFoundException fnf){
			System.out.println("Specified file not found :" + fnf);
		}
		catch(IOException ioe){
			System.out.println("Error while copying file :" + ioe);
		}
	}
	
	
	/**
	* convert certificate file path from string to byteArrayInput stream
	*
	* @param fname certificate file path
	* @return byteArrayInput stream form of the certificate file path
	*/
	public InputStream fullStream(String fname) throws IOException {
		FileInputStream fis = new FileInputStream(fname);
		DataInputStream dis = new DataInputStream(fis);
		byte[] bytes = new byte[dis.available()];
		dis.readFully(bytes);
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		dis.close();
		return bais;
	}
}