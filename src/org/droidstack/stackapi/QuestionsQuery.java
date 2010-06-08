package org.droidstack.stackapi;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Used to build question API queries
 * @author Felix Oghină <felix.oghina@gmail.com>
 *
 */
public class QuestionsQuery {
	
	public final static String QUERY_ALL = "/questions";
	public final static String QUERY_UNANSWERED = "/questions/unanswered";
	public final static String QUERY_USER = "/users/{id}/questions";
	public final static String QUERY_FAVORITES = "/users/{id}/favorites";

	public final static String SORT_ACTIVITY = "activity";
	public final static String SORT_VOTES = "votes";
	public final static String SORT_CREATION = "creation";
	public final static String SORT_FEATURED = "featured";
	public final static String SORT_HOT = "hot";
	public final static String SORT_WEEK = "week";
	public final static String SORT_MONTH = "month";
	public final static String SORT_VIEWS = "views";
	public final static String SORT_ADDED = "added";
	
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
	
	/**
	 * Builds a new QuestionQuery object
	 * @param type One of the <code>QUERY_*</code> constants
	 * @throws IllegalArgumentException If <b>type</b> is not one of the <code>QUERY_*</code> constants 
	 */
	public QuestionsQuery(String type) throws IllegalArgumentException {
		if (!type.equals(QUERY_ALL) && !type.equals(QUERY_UNANSWERED) && !type.equals(QUERY_USER) && !type.equals(QUERY_FAVORITES)) {
			throw new IllegalArgumentException("Invalid type argument passed to QuestionQuery constructor.");
		}
		mQueryType = type;
		
		validSortFields.put(QUERY_ALL,
			new String[] { SORT_ACTIVITY, SORT_VOTES, SORT_CREATION, SORT_FEATURED, SORT_HOT, SORT_WEEK, SORT_MONTH });
		validSortFields.put(QUERY_UNANSWERED,
			new String[] { SORT_CREATION, SORT_VOTES });
		validSortFields.put(QUERY_USER,
			new String[] { SORT_ACTIVITY, SORT_VIEWS, SORT_CREATION, SORT_VOTES });
		validSortFields.put(QUERY_FAVORITES,
			new String[] { SORT_ACTIVITY, SORT_VIEWS, SORT_CREATION, SORT_ADDED, SORT_VOTES });
		
		setSort(validSortFields.get(type)[0]);
		setOrder(ORDER_DESCENDING);
	}
	
	/**
	 * Set the user ID. This is used for query types that need a user ID, such as <code>QUERY_USER</code> or <code>QUERY_FAVORITES</code>
	 * @param userID The user ID to use
	 * @return The same object, to allow chaining
	 */
	public QuestionsQuery setUser(long userID) {
		mUserID = userID;
		return this;
	}
	
	/**
	 * Sets the sort field to use. If you do not call this method, the default sort field for the selected query type will be used 
	 * @param sort One of the <code>SORT_*</code> constants
	 * @see #setOrder(String)
	 * @return The same object, to allow chaining
	 * @throws IllegalArgumentException If the <b>sort</b> parameter is not valid for the selected query type
	 */
	public QuestionsQuery setSort(String sort) throws IllegalArgumentException {
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
	
	/**
	 * Sets the order to use (ascending/descending)
	 * @param order One of the <code>ORDER_*</code> constants
	 * @see #setSort(String)
	 * @return The same object, to allow chaining
	 * @throws IllegalArgumentException If the <b>order</b> parameter is not one of the <code>ORDER_*</code> constants
	 */
	public QuestionsQuery setOrder(String order) throws IllegalArgumentException {
		if (!order.equals(ORDER_DESCENDING) && !order.equals(ORDER_ASCENDING)) {
			throw new IllegalArgumentException("Invalid order specified");
		}
		mOrder = order;
		return this;
	}
	
	/**
	 * Sets the number of the page to get.
	 * @param page The page number
	 * @see #setPageSize(int)
	 * @return The same object, to allow chaining
	 */
	public QuestionsQuery setPage(int page) {
		mPage = page;
		return this;
	}
	
	/**
	 * Sets the number of questions to be returned per page
	 * @param pageSize The number of questions
	 * @see #setPage(int)
	 * @return The same object, to allow chaining
	 */
	public QuestionsQuery setPageSize(int pageSize) {
		mPageSize = pageSize;
		return this;
	}
	
	/**
	 * Builds a query path according to the current settings.
	 * @return The path as a string (e.g. "/questions?sort=hot")
	 * @throws IllegalStateException If the selected query type requires a user ID but no user ID was set
	 */
	public String buildQueryPath() throws IllegalStateException {
		String url = mQueryType;
		if (mQueryType.equals(QUERY_USER) || mQueryType.equals(QUERY_FAVORITES)) {
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
	
	/**
	 * Get the current sort field
	 * @return One of the <code>SORT_*</code> constants
	 */
	public String getSort() {
		return mSort;
	}
	
	/**
	 * Get the current order
	 * @return One of the <code>ORDER_*</code> constants
	 */
	public String getOrder() {
		return mOrder;
	}
	
}
