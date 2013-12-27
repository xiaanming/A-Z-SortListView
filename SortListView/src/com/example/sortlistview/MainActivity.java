package com.example.sortlistview;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.sortlistview.SideBar.OnTouchingLetterChangedListener;

public class MainActivity extends Activity {
	private ListView sortListView;
	private SideBar sideBar;
	private TextView dialog;
	private SortAdapter adapter;
	private ClearEditText mClearEditText;
	
	/**
	 * 汉字转换成拼音的类
	 */
	private CharacterParser characterParser;
	private List<SortModel> SourceDateList;
	
	/**
	 * 根据拼音来排列ListView里面的数据类
	 */
	private PinyinComparator pinyinComparator;
	
	private ProgressDialog mProgressDialog;
	
	
	private static final String PATH = "http://220.231.15.134/searchLeftMenuPad.jsp?word=汽车";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initViews();
	}
	
	private Handler mHandler = new Handler(){  

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			//关闭进度条
			mProgressDialog.dismiss();
			if((String)msg.obj == null){
				Toast.makeText(MainActivity.this, "获取数据失败", Toast.LENGTH_SHORT).show();
				return;
			}
			
			String result = (String) msg.obj;
			try {
				SourceDateList = filledData(getCuntryData(result));
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			// 根据a-z进行排序源数据
			Collections.sort(SourceDateList, pinyinComparator);
			adapter = new SortAdapter(MainActivity.this, SourceDateList);
			sortListView.setAdapter(adapter);
			
			
		}
		
	};
	
	
	/***********
	 * 
	 * @param result
	 * @return这个地方的解析很有必要看一下
	 * @throws JSONException
	 */
	private List<SortModel> getCuntryData(String result) throws JSONException{
		if(result == null){
			return null;
		}
		
		JSONObject json = new JSONObject(result);
		List<SortModel> list = new ArrayList<SortModel>();
		for(Iterator<String> it = json.keys(); it.hasNext();){
			String str = it.next();
			JSONArray jsonArray = json.optJSONArray(str);
			for(int i=0; i<jsonArray.length(); i++){
				JSONObject jsonObject = jsonArray.optJSONObject(i);
				SortModel sortModel = new SortModel();
				if("汽车".equals(str)){
					sortModel.setName(jsonObject.optString("Brand"));
					sortModel.setSortLetters(jsonObject.optString("Category"));
				}
				
				
				list.add(sortModel);
			}
			
		}
		
		
		return list;
		
	}


	private void initViews() {
		showProgressDialog();
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				HttpClient httpClient=new DefaultHttpClient();
				HttpGet httpGet = new HttpGet(PATH);
				String result = null;
				try {
					HttpResponse httpResponse = httpClient.execute(httpGet);
					if(httpResponse.getStatusLine().getStatusCode() == 200){
						result = responseHandler(httpResponse, "UTF-8");
						
						System.out.println(result);
					}
				}  catch (IOException e) {
					e.printStackTrace();
					
				} finally{
					httpGet.abort();
					
					//将网络数据发送给Handler
					Message msg = mHandler.obtainMessage();
					msg.obj = result;
					mHandler.sendMessage(msg);
				}
			}
		}).start();
		
		
		//实例化汉字转拼音类
		characterParser = CharacterParser.getInstance();
		
		pinyinComparator = new PinyinComparator();
		
		sideBar = (SideBar) findViewById(R.id.sidrbar);
		dialog = (TextView) findViewById(R.id.dialog);
		sideBar.setTextView(dialog);
		
		//设置右侧触摸监听
		sideBar.setOnTouchingLetterChangedListener(new OnTouchingLetterChangedListener() {
			
			@Override
			public void onTouchingLetterChanged(String s) {
				//该字母首次出现的位置
				int position = adapter.getPositionForSection(s.charAt(0));
				if(position != -1){
					sortListView.setSelection(position);
				}
				
			}
		});
		
		sortListView = (ListView) findViewById(R.id.country_lvcountry);
		sortListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				//这里要利用adapter.getItem(position)来获取当前position所对应的对象
				Toast.makeText(getApplication(), ((SortModel)adapter.getItem(position)).getName(), Toast.LENGTH_SHORT).show();
			}
		});
		
		
		
		
		mClearEditText = (ClearEditText) findViewById(R.id.filter_edit);
		
		//根据输入框输入值的改变来过滤搜索
		mClearEditText.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				//当输入框里面的值为空，更新为原来的列表，否则为过滤数据列表
				filterData(s.toString());
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
			}
		});
	}

	/**
	 * 显示ProgressDialog
	 */
	private void showProgressDialog(){
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setMessage("数据加载中...");
		mProgressDialog.show();
	}

//	/**
//	 * 为ListView填充数据
//	 * @param date
//	 * @return
//	 */
//	private List<SortModel> filledData(List<so> list){
//		final List<SortModel> mSortList = new ArrayList<SortModel>();
//		
//		for(CountryBean countryBean : list){
//			SortModel sortModel = new SortModel();
//			sortModel.setName(countryBean.getBrand());
//			//汉字转换成拼音
//			String pinyin = characterParser.getSelling(countryBean.getBrand());
//			String sortString = pinyin.substring(0, 1).toUpperCase();
//			
//			// 正则表达式，判断首字母是否是英文字母
//			if(sortString.matches("[A-Z]")){
//				sortModel.setSortLetters(sortString.toUpperCase());
//			}else{
//				sortModel.setSortLetters("#");
//			}
//			
//			mSortList.add(sortModel);
//		}
//		
//		return mSortList;
//		
//	}
	
	/**
	 * 根据输入框中的值来过滤数据并更新ListView
	 * @param filterStr
	 */
	private void filterData(String filterStr){
		List<SortModel> filterDateList = new ArrayList<SortModel>();
		
		if(TextUtils.isEmpty(filterStr)){
			filterDateList = SourceDateList;
		}else{
			filterDateList.clear();
			for(SortModel sortModel : SourceDateList){
				String name = sortModel.getName();
				if(name.indexOf(filterStr.toString()) != -1 || characterParser.getSelling(name).startsWith(filterStr.toString())){
					filterDateList.add(sortModel);
				}
			}
		}
		
		// 根据a-z进行排序
		Collections.sort(filterDateList, pinyinComparator);
		adapter.updateListView(filterDateList);
	}
	
	
	/**
	 * 处理Http请求的结果
	 * @param response
	 * @param encoding
	 * @return
	 */
	public String responseHandler(HttpResponse response, String encoding) {
		StringBuffer sb = new StringBuffer();
		HttpEntity httpEntity = response.getEntity();
		BufferedReader br = null;
		try{
			br = new BufferedReader(new InputStreamReader(
					httpEntity.getContent(), encoding));
			String line = null;
			while((line = br.readLine()) != null){
				sb.append(line);
			}
			
			return sb.toString();
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(br != null){
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return null;

	}
}
