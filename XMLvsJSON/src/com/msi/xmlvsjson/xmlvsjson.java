/*
 * 
 * W. F. Ableson
 * fableson@msiservices.com
 * 
 */
package com.msi.xmlvsjson;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.view.*;
import java.io.*;
import org.json.*;
import android.content.res.Resources;


import javax.xml.parsers.SAXParser;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;

import org.xml.sax.XMLReader;



public class xmlvsjson extends Activity {
	Button btnXML;
	Button btnJSON;
	TextView tvData;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        
        tvData = (TextView) findViewById(R.id.txtData);
        btnXML = (Button) findViewById(R.id.btnXML);
        btnXML.setOnClickListener(new Button.OnClickListener()
        {
        	public void onClick(View v)
        	{ 
        		examineXMLFile();
        	}
        });
        

        btnJSON = (Button) findViewById(R.id.btnJSON);
        btnJSON.setOnClickListener(new Button.OnClickListener()
        {
        	public void onClick(View v)
        	{
        		examineJSONFile();
        	}
        });
        
    }
    
    void examineXMLFile()
    {
    	try {
    		InputSource is = new InputSource(getResources().openRawResource(R.raw.xmltwitter));
    			
	        // create the factory
	        SAXParserFactory factory = SAXParserFactory.newInstance();
	
	        // create a parser
	        SAXParser parser = factory.newSAXParser();

	        // create the reader (scanner)
	        XMLReader xmlreader = parser.getXMLReader();

	        // instantiate our handler
	        twitterFeedHandler tfh = new twitterFeedHandler();

	        // assign our handler
	        xmlreader.setContentHandler(tfh);

	        // perform the synchronous parse
	        xmlreader.parse(is);
	        
	        // should be done... let's display our results
	        tvData.setText(tfh.getResults());
    	}
    	catch (Exception e) {
    		tvData.setText(e.getMessage());
    	}

    }
    
    void examineJSONFile()
    {
    	try
    	{
    		String x = "";
    		InputStream is = this.getResources().openRawResource(R.raw.jsontwitter);
    		byte [] buffer = new byte[is.available()];
    		while (is.read(buffer) != -1);
    		String jsontext = new String(buffer);
    		JSONArray entries = new JSONArray(jsontext);
    		
    		x = "JSON parsed.\nThere are [" + entries.length() + "]\n\n";
    		
    		int i;
    		for (i=0;i<entries.length();i++)
    		{
    			JSONObject post = entries.getJSONObject(i);
    			x += "------------\n";
    			x += "Date:" + post.getString("created_at") + "\n";
    			x += "Post:" + post.getString("text") + "\n\n";
    		}
    		tvData.setText(x);
    	}
    	catch (Exception je)
    	{
    		tvData.setText("Error w/file: " + je.getMessage());
    	}
    }
    
}