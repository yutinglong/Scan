package com.zxing.scan;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class JSonUtils {

	public static String getString(JSONObject object, String propname) {
		String result = "";
		try {
			if (object != null && object.containsKey(propname)) {
				Object value = object.get(propname);
				if (value != null) {
					if (value instanceof String) {
						result = (String) value;
					} else {
						result = String.valueOf(value);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public static Integer getInt(JSONObject object, String propname) {
		return getIntDefault(object, propname, 0);
	}

	public static Integer getIntDefault(JSONObject object, String propname,
			int defaultvalue) {
		int result = defaultvalue;
		try {
			if (object != null && object.containsKey(propname)) {
				Object value = object.get(propname);
				if (value != null) {
					if (value instanceof Integer) {
						result = ((Integer) value).intValue();
					} else if (value instanceof Float) {
						result = ((Float) value).intValue();
					} else if (value instanceof Double) {
						result = ((Double) value).intValue();
					} else if (value instanceof Long) {
						result = ((Long) value).intValue();
					} else if (value instanceof String) {
						String temp = (String) value;
						if (temp.contains(".")) {
							temp = temp.substring(0, temp.lastIndexOf("."));
						}
						result = Integer.parseInt(temp);
					}
				}
			}
		} catch (Exception e) {
		}
		return result;
	}

	public static Double getDouble(JSONObject object, String propname) {
		double reslut = -1.0d;
		try {
			if (object != null && object.containsKey(propname)) {
				String prop = (String) object.get(propname);
				reslut = Double.parseDouble(prop);
			}
		} catch (Exception e) {
		}

		return reslut;
	}

	public static Boolean getBoolean(JSONObject object, String propname) {
		boolean result = false;
		try {
			if (object != null && object.containsKey(propname)) {
				Object o = object.get(propname);
				if (o != null) {
					if (o instanceof Boolean) {
						result = (Boolean) o;
					} else if (o instanceof String) {
						result = Boolean.valueOf(o.toString());
					}
				}
			}
		} catch (Exception e) {
		}

		return result;
	}

	public static JSONObject getJSonObject(JSONObject object, String propname) {
		try {
			if (object != null && object.containsKey(propname)) {
				return (JSONObject) object.get(propname);
			}
		} catch (Exception e) {
		}
		return null;
	}

	public static JSONArray getJSonArray(JSONObject object, String propname) {
		JSONArray array = new JSONArray();
		try {
			if (object != null && object.containsKey(propname)) {
				Object o = object.get(propname);
				if (o != null && o instanceof JSONArray) {
					return (JSONArray) o;
				}
			}
		} catch (Exception e) {
			return array;
		}
		return array;
	}

	public static JSONObject getJSONObject(String content) {
		JSONObject object = null;
		try {
			object = (JSONObject) JSONValue.parse(content);
		} catch (Exception e) {
			// TODO: handle exception
		}
		return object;
	}

	public static Long getLong(JSONObject object, String propname) {
		long result = 0;
		try {
			if (object != null && object.containsKey(propname)) {
				Object value = object.get(propname);
				if (value != null) {
					if (value instanceof Float) {
						result = ((Float) value).intValue();
					} else if (value instanceof Double) {
						result = ((Double) value).intValue();
					} else if (value instanceof Long) {
						result = ((Long) value).intValue();
					} else if (value instanceof String) {
						String temp = (String) value;
						if (temp.contains(".")) {
							temp = temp.substring(0, temp.lastIndexOf("."));
						}
						result = Integer.parseInt(temp);
					}
				}
			}
		} catch (Exception e) {
		}
		return result;
	}

}
