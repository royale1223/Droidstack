package org.droidstack.activity;

import org.droidstack.R;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ChatActivity extends Activity {
	
	private boolean isLoading = false;
	
	private String mEndpoint;
	private String mSiteName;
	private String mChatURL;
	
	private LinearLayout titleLayout;
	private TextView title;
	private WebView mWebView;
	
	/**
	 * Resolve the URL to the chat interface of a specific site by its API endpoint.
	 * For example, <code>http://api.stackoverflow.com</code> is transformed into
	 * <code>http://chat.stackoverflow.com</code>
	 * @param endpoint the endpoint of the StackExchange site
	 * @return the chat interface URL.
	 */
	public static String getChatUrl(String endpoint) {
		return endpoint.replace("api.", "chat.");
	}
	
	@Override
	protected void onCreate(Bundle inState) {
		super.onCreate(inState);
		setContentView(R.layout.chat);

		titleLayout = (LinearLayout) findViewById(R.id.title_layout);
		title = (TextView) findViewById(R.id.title);
		mWebView = (WebView) findViewById(R.id.content);
		
		mWebView.setWebChromeClient(new ChatChromeClient());
		mWebView.setWebViewClient(new ChatWebClient());
		
		Uri data = getIntent().getData();
		mEndpoint = data.getQueryParameter("endpoint");
		mSiteName = data.getQueryParameter("name");
		
		if (mSiteName != null) setTitle(mSiteName + " Chat");
		else setTitle("Chat");
		
		mChatURL = getChatUrl(mEndpoint);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.loadUrl(mChatURL);
	}
	
	@Override
	public void setTitle(CharSequence title) {
		this.title.setText(title);
	}
	
	@Override
	public void setTitle(int titleId) {
		setTitle(getString(titleId));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.chat, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.menu_close:
			finish();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			if (mWebView.canGoBack()) mWebView.goBack();
			else finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private class ChatWebClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			return false;
		}
		
	}
	
	private class ChatChromeClient extends WebChromeClient {
		@Override
		public void onProgressChanged(WebView view, int newProgress) {
			if (newProgress < 100 && !isLoading) {
				isLoading = true;
				titleLayout.setVisibility(View.VISIBLE);
			}
			if (newProgress == 100 && isLoading) {
				isLoading = false;
				titleLayout.setVisibility(View.GONE);
			}
		}
	}
	
}