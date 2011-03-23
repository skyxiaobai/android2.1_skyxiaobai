package JsonDemo2.com;

import java.io.InputStream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONStringer;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class JsonDemo2 extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        String x;
        JSONObject obj;
        try {

            InputStream is = this.getResources().openRawResource(R.raw.json);  
            byte [] buffer = new byte[is.available()] ;
            is.read(buffer);
            TextView v = new TextView(this);
                    
            String json = new String(buffer,"utf-8");   

            obj = new JSONObject(json);  
                        
            x = obj.getString("姓名");                           
            Log.d("======姓名========",x);
            x = obj.getString("性别");                           
            Log.d("======性别========",x);
            x = obj.getString("年龄");                           
            Log.d("======年龄========",x);
               
            JSONObject obj1 = obj.getJSONObject("学习成绩");
            x = obj1.getString("数学");                           
            Log.d("======数学========",x);
            x = obj1.getString("语文");                           
            Log.d("======语文========",x);
            x = obj1.getString("英语");                           
            Log.d("======英语========",x);
            
            JSONArray array = obj1.getJSONArray("综合");   
            obj = array.getJSONObject(0);
            x = obj.getString("文科综合");                           
            Log.d("======文科综合========",x);
            obj = array.getJSONObject(1);
            x = obj.getString("理科综合");                           
            Log.d("======理科综合========",x);
            
            
            JSONStringer s = new JSONStringer();
            Log.d("======================",s.object().key("a").value("aaa").endObject().toString());
            
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
}
}