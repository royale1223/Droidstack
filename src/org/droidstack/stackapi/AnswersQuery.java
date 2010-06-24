package org.droidstack.stackapi;

import java.util.ArrayList;
import java.util.HashMap;

public class AnswersQuery {
	
	public final static String QUERY_USER = "/users/{id}/answers";
	
	public final static String SORT_ACTIVITY = "activity";
	public final static String SORT_VIEWS = "views";
	public final static String SORT_CREATION = "creation";
	public final static String SORT_VOTES = "votes";
	
	public final static String ORDER_DESCENDING = "desc";
	public final static String ORDER_ASCENDING = "asc";
	
	private final static String KEY_SORT = "sort";
	private final static String KEY_ORDER = "order";
	private final static String KEY_PAGE = "page";
	private final static String KEY_PAGESIZE = "pagesize";
	
	public final static HashMap<String, String[]> validSortFields = new HashMap<String, String[]>();
	
	private final String mQueryType;
	private long mUserID;
	private String mSort;
	private String mOrder;
	private int mPage;
	private int mPageSize;
	
	public AnswersQuery(String type) throws IllegalArgumentException {
		if (!type.equals(QUERY_USER)) {
			throw new IllegalArgumentException("Invalid type argument passed to AnswersQuery constructor.");
		}
		mQueryType = type;
		
		validSortFields.put(QUERY_USER,
			new String[] { SORT_ACTIVITY, SORT_VIEWS, SORT_CREATION, SORT_VOTES });
		
		setSort(validSortFields.get(type)[0]);
		setOrder(ORDER_DESCENDING);
	}
	
	public AnswersQuery setUser(long userID) {
		mUserID = userID;
		return this;
	}
	
	public AnswersQuery setSort(String sort) throws IllegalArgumentException {
		int i;
		String[] sortFields = validSortFields.get(mQueryType);
		for (i=0; i < sortFields.length; i++) {
			if (sortFields[i].equals(sort)) {
				mSort = sort;
				return this;
			}
		}
		throw new IllegalArgumentException("Sort field incorrect for this query type");
	}
	
	public AnswersQuery setOrder(String order) throws IllegalArgumentException {
		if (!order.equals(ORDER_DESCENDING) && !order.equals(ORDER_ASCENDING)) {
			throw new IllegalArgumentException("Invalid order specified");
		}
		mOrder = order;
		return this;
	}
	
	public AnswersQuery setPage(int page) {
		mPage = page;
		return this;
	}
	
	public AnswersQuery setPageSize(int pageSize) {
		mPageSize = pageSize;
		return this;
	}
	
	public String buildQueryPath() throws IllegalStateException {
		String url = mQueryType;
		if (mQueryType.equals(QUERY_USER)) {
			if (mUserID == 0) {
				throw new IllegalStateException("The selected query type requires a user ID");
			}
			url = url.replace("{id}", String.valueOf(mUserID));
		}
		ArrayList<String> params = new ArrayList<String>();
		if (mSort != null) params.add(KEY_SORT + "=" + mSort);
		if (mOrder != null) params.add(KEY_ORDER + "=" + mOrder);
		if (mPage != 0) params.add(KEY_PAGE + "=" + String.valueOf(mPage));
		if (mPageSize != 0) params.add(KEY_PAGESIZE + "=" + String.valueOf(mPageSize));
		if (params.size() > 0) {
			StringBuilder builder = new StringBuilder();
			for (String param: params) {
				builder.append(param).append('&');
			}
			url += "?" + builder.toString();
		}
		return url;
	}
	
	public String getSort() {
		return mSort;
	}
	
	public String getOrder() {
		return mOrder;
	}
	
}
