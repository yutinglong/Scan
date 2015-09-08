package com.zxing.scan.zxing.data;

import java.io.Serializable;

/**
 * 图书或CD扫描结果实体类
 * 
 * @author yutinglong
 */
public class BookOrCDEntity implements Serializable{

	private static final long serialVersionUID = 1L;
	public String netUrl;	// 网址
	public String webData;	// 网页数据HTML+CSS
	
	public String name;
	public String typeName;// CD、图书
	
	public BookOrCDEntity(){
	}
}
