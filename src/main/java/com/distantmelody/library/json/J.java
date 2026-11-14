package com.distantmelody.library.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class J
{
	int ARRAY = 0;
	int MAP = 1;
	
	int type;
	
	ArrayList<Object> arrayValues;
	Map<String,J> mapValues;
	private boolean arrayHint;
	
	private J()
	{
		
	}
	
	private J(boolean map, Object... d)
	{
		if (map)
		{
			type = MAP;
			mapValues = new HashMap<String,J>();
			for (int i = 0; i < d.length;i+=2)
			{
				Object v = d[i+1];
				if (!(v instanceof J))
				{
					v = new J(false,v);
				}
				else if (((J)v).isMap())
				{
					v = new J(false,v);
				}
				mapValues.put((String)d[i], (J)v);
			}
		}
		else
		{
			type = ARRAY;
			arrayValues = new ArrayList<Object>();
			if (d == null)
			{
				arrayValues.add(null);
			}
			else
			{
				for (Object o:d)
				{
					if (o != null && o.getClass().isArray())
					{
						int length =  Array.getLength(o);
						for (int i = 0; i < length;i++)
						{
							arrayValues.add(Array.get(o, i));
						}
						arrayHint = true;
					}
					else if (o != null && o instanceof List) 
					{
						for (Object o2 : (List<?>) o) 
						{
							arrayValues.add(o2);
				        }
						arrayHint = true;
				    }
					else
					{
						arrayValues.add(o);
					}
				}
			}
		}
	}
	
	public static J A(Object... d){	J ret = new J(false,d); ret.arrayHint = true;return ret;}
	private static J A_INTERNAL(Object... d){return new J(false,d);}
	public static J M(Object... d){return new J(true,d);}
	
	public static J fromString(String s)
	{
		StringBuilder kept = new StringBuilder(s.length());
		boolean inString = false;
		for (int i = 0; i < s.length();i++)
		{
			char c = s.charAt(i);
			if (c == '"' && !escaped(s, i))
			{
				inString = !inString;
				kept.append(c);
				continue;
			}
			if (!inString && (c == ' ' || c == '\n' || c == '\r' || c == '\t'))
			{
				continue;
			}
			kept.append(c);
		}
		return new J().fromStringInternal(kept.toString());
	}
	
	private J fromStringInternal(String s)
	{
		if (s.startsWith("{"))
		{
			type = MAP;
			s = s.substring(1,s.length()-1);
			mapValues = getMap(s);
		}
		else
		{
			type = ARRAY;
			if (s.startsWith("["))
			{
				s = s.substring(1,s.length()-1);
				arrayHint = true;
			}
			arrayValues = getArray(s);
		}
		return this;
	}
	
	private ArrayList<Object> getArray(String s) {
		ArrayList<Object> ret = new ArrayList<Object>();
		ArrayList<String> elements = getElements(s);
		for (String e:elements)
		{
			//System.out.println("e:"+e);
			if (e.startsWith("{") || e.startsWith("["))//second part might not be needed. dont think it would be legal json
			{
				ret.add(new J().fromStringInternal(e));
			}
			else if (e.startsWith("\""))
			{
				ret.add(unescape(e.substring(1,e.length()-1)));
			}
			else if (e.toLowerCase().equals("true") || e.toLowerCase().equals("false"))
			{
				ret.add(Boolean.parseBoolean(e));
			}
			else if (e.toLowerCase().equals("null"))
			{
				ret.add(null);
			}
			else
			{
				try
				{
					ret.add(new BigDecimal(Double.parseDouble(e)));
				}
				catch (Exception e2)
				{
					System.out.println("failed to parse " + e);
					//ret.add(new BigDecimal(0));
					throw new RuntimeException("failed to parse " + e);
				}
			}
		}
		return ret;
	}

	private Map<String, J> getMap(String s) 
	{
		HashMap<String,J> ret = new HashMap<String,J>();
		ArrayList<String> elements = getElements(s);
		for (String e:elements)
		{
			int colon = keyColon(e);
			J newj = new J().fromStringInternal(e.substring(colon+1,e.length()));
			if (newj.isMap())
			{
				newj = J.A_INTERNAL(newj);
			}
			ret.put(unescape(e.substring(1, colon-1)), newj);
		}
		return ret;
	}
	
	public static ArrayList<String> getElements(String r) 
	{
		ArrayList<String> ret = new ArrayList<String>();
		
		int level = 0;
		boolean inString = false;
		int count = 0;
		while (r.length() > 0)
		{
			if (r.charAt(count) == '"' && !escaped(r, count))
			{
				inString = !inString;
			}
			if (!inString && (r.charAt(count) == '{' ||  r.charAt(count) == '['))
			{
				level++;
			}
			if (!inString && (r.charAt(count) == '}' ||  r.charAt(count) == ']'))
			{
				level--;
			}
			if (!inString && level == 0 && (r.charAt(count) == ',' || count == r.length()-1))
			{
				int take = count;
				if (count == r.length()-1)
				{
					take++;
				}
				String s = r.substring(0,take);
				ret.add(s);

				r = r.substring(count+1);
				count = 0;
			}
			else
			{
				count++;
			}
		}
		return ret;
	}

	public String toString()
	{
		String ret = "";
		if (type == ARRAY)
		{
			for (int i = 0; i < arrayValues.size();i++)
			{
				Object e = arrayValues.get(i);
				String v = e+"";
				if (e instanceof String)
				{
					v = "\""+escape((String)e)+"\"";
				}
				if (i < arrayValues.size()-1)
				{
					ret += v + ",";
				}
				else
				{
					ret += v;
				}	
			}
			if (arrayValues.size() > 1 || arrayHint)
			{
				ret = "["+ret+"]";
			}
			if (ret.length()==0)
			{
				ret = "[]";
			}
		}
		else//type == MAP
		{
			int i = 0;
			for (String k: mapValues.keySet())
			{
				Object e = mapValues.get(k);
				String v = e.toString();
				if (i < mapValues.size()-1)
				{
					ret += "\""+escape(k)+"\":"+v + ",";
				}
				else
				{
					ret += "\""+escape(k)+"\":"+v;
				}
				i++;
			}
			ret = "{"+ret+"}";
		}
		return ret;
	}
	
	public Object[] values()
	{
		/*
		if (type == ARRAY)
		{
			return arrayValues.toArray();
		}
		else//type == map
		{
			return mapValues.values().toArray();
		}
		*/
		return arrayValues.toArray();
	}
	public J[] valuesj()
	{
		//TODO why not just return (J[])values()? check later; 
		Object[] v = values();
		J[] ret = new J[v.length];
		for (int i = 0; i < v.length;i++)
		{
			ret[i] = (J) v[i];
		}
		return ret;
	}
	
	public J get(String name)
	{
		if (isMap())
		{
			return mapValues.get(name);
		}
		return getJ(0).getJ(name);
	}
	

	public boolean contains(String name) {
		if (isMap())
		{
			return mapValues.containsKey(name);
		}
		if (arrayValues.size() == 0)
		{
			return false;
		}
		return getJ(0).contains(name);
	}
	
	public boolean containsValue(Object value) {
		if (isMap())
		{
			return false;
		}
		return arrayValues.contains(value);
	}
	

	public void remove(String name) {
		if (isMap())
		{
			mapValues.remove(name);
			return;
		}
		getJ(0).remove(name);
	}
	
	public void remove(int i) {
		arrayValues.remove(i);
	}
	
	public Object get(int index)
	{
		return arrayValues.get(index);
	}
	public Object get()
	{
		return arrayValues.get(0);
	}
	
	public J getJ(int index)
	{
		return (J)get(index);
	}
	
	public J getJ(String name)
	{
		return (J)get(name);
	}
	
	public String[] keys()
	{
		if (!isMap())return getJ(0).keys();
		String ret[] = new String[mapValues.size()];
		int i = 0;
		for (String k:mapValues.keySet())
		{
			ret[i] = k;
			i++;
		}
		return ret;
	}
	
	public boolean isMap()
	{
		return type == MAP;
	}
	
	
	public int getInt()
	{
		return number(get(0)).intValue();
	}
	public BigDecimal getBigDecimal()
	{
		return bigDecimal(get(0));
	}
	public int getInt(int index)
	{
		return number(get(index)).intValue();
	}
	public float getFloat(int index)
	{
		return number(get(index)).floatValue();
	}
	public long getLong(int index)
	{
		return number(get(index)).longValue();
	}
	public BigDecimal getBigDecimal(int index)
	{
		return bigDecimal(get(index));
	}
	public String getString()
	{
		return (String)get(0);
	}
	
	public String getString(int index)
	{
		return (String)get(index);
	}
	
	public String getString(String name)
	{
		J j = get(name);
		if (j == null)return null;
		return (String)j.get(0);
	}

	public String getString(String name, String defaultValue)
	{
		if (!contains(name))return defaultValue;
		return getString(name);
	}
	
	public String getString(String name, int index)
	{
		return (String)get(name).get(index);
	}
	
	public int getInt(String name)
	{
		return number(get(name).get(0)).intValue();
	}
	
	public int getInt(String name,int index)
	{
		return number(get(name).get(index)).intValue();
	}
	
	public float getFloat(String name)
	{
		return number(get(name).get(0)).floatValue();
	}

	public float getFloat(String name, float defaultValue)
	{
		if (!contains(name))return defaultValue;
		return getFloat(name);
	}
	
	public long getLong(String name)
	{
		return number(get(name).get(0)).longValue();
	}

	public long getLong(String name, long defaultValue)
	{
		if (!contains(name))return defaultValue;
		return getLong(name);
	}
	
	public float getFloat(String name,int index)
	{
		return number(get(name).get(index)).floatValue();
	}
	
	public BigDecimal getBigDecimal(String name)
	{
		J j = get(name);
		if (j == null)return null;
		return bigDecimal(j.get(0));
	}

	public BigDecimal getBigDecimal(String name, BigDecimal defaultValue)
	{
		if (!contains(name))return defaultValue;
		return getBigDecimal(name);
	}
	
	public BigDecimal getBigDecimal(String name, int index)
	{
		return bigDecimal(get(name).get(index));
	}
	
	public boolean getBoolean(int index)
	{
		return (Boolean)get(index);
	}
	
	public Boolean getBoolean(String name,boolean nullVal)
	{
		if (!contains(name))return nullVal;
		return getBoolean(name);
	}
	
	public Boolean getBoolean(String name)
	{
		J j = get(name);
		if (j == null)return null;
		return (Boolean)j.get(0);
	}
	
	public boolean getBoolean(String name, int index)
	{
		return (Boolean)get(name).get(index);
	}
	
	public boolean getBoolean()
	{
		return (boolean)get(0);
	}
	
	public void add(Object v)
	{
		arrayValues.add(v);
	}
	
	public void add(int index,Object v)
	{
		arrayValues.add(index,v);
	}
	
	public void put(String key, Object v)
	{
		J value = null;
		if (v instanceof J)
		{
			value = (J) v;
			if (value.isMap())
			{
				value = J.A_INTERNAL(value);
			}
		}
		else
		{
			value = J.A_INTERNAL(v);
		}
		if (mapValues != null)
		{
			mapValues.put(key, value);	
		}
		else
		{
			getJ(0).put(key, value);
		}
	}

	public int getMapSize() {
		if (isMap())
		{
			return mapValues.size();
		}
		else
		{
			return getJ(0).getMapSize();
		}
	}
	
	public int getArraySize()
	{
		if (arrayValues == null) return 0;
		return arrayValues.size();
	}
	

	public static J fromInputStream(InputStream is) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		byte[] data = new byte[32];

		while ((nRead = is.read(data, 0, data.length)) != -1) {
		  buffer.write(data, 0, nRead);
		}

		buffer.flush();
		String s = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
		return fromString(s);
	}

	public void write(OutputStream outputStream) throws IOException {
		outputStream.write(toString().getBytes(StandardCharsets.UTF_8));
	}

	public static J fromBytes(byte[] data) 
	{
		return fromString(new String(data, StandardCharsets.UTF_8));
	}

	public int[] getIntArray() {
		int[] ret = new int[getArraySize()];
		int i = 0;
		for (Object val: values())
		{
			ret[i] = number(val).intValue();
			i++;
		}	
		return ret;
	}
	
	public float[] getFloatArray() {
		float[] ret = new float[getArraySize()];
		int i = 0;
		for (Object val: values())
		{
			ret[i] = number(val).floatValue();
			i++;
		}	
		return ret;
	}
	
	public int[] getIntArray(String name)
	{
		return getJ(name).getIntArray();
	}
	
	public float[] getFloatArray(String name)
	{
		return getJ(name).getFloatArray();
	}

	public void set(int index, Object value) {
		arrayValues.set(index, value);
	}

	public Integer getInteger()
	{
		return getInteger(0);
	}
	public Integer getInteger(int index)
	{	
		Object value = get(index);
		if (value == null)return null;
		return Integer.valueOf(number(value).intValue());
	}
	
	public Integer getInteger(String name)
	{
		J j = get(name);
		if (j == null)return null;
		Object value =j.get(0);
		if (value == null)return null;
		return Integer.valueOf(number(value).intValue());
	}

	public Integer getInteger(String name, Integer defaultValue)
	{
		if (!contains(name))return defaultValue;
		return getInteger(name);
	}

	public void clearMap() {
		if (isMap())
		{
			mapValues.clear();
		}
		else 
		{
			getJ(0).clearMap();
		}
	}
	
	public void clearAarray() {
		arrayValues.clear();
	}

	private static boolean escaped(String s, int index)
	{
		int slashes = 0;
		for (int i = index - 1; i >= 0 && s.charAt(i) == '\\'; i--)
		{
			slashes++;
		}
		return slashes % 2 == 1;
	}

	private static int keyColon(String element)
	{
		boolean inString = false;
		for (int i = 0; i < element.length(); i++)
		{
			char c = element.charAt(i);
			if (c == '"' && !escaped(element, i))
			{
				inString = !inString;
				continue;
			}
			if (!inString && c == ':')
			{
				return i;
			}
		}
		throw new RuntimeException("failed to parse " + element);
	}

	private static String unescape(String raw)
	{
		StringBuilder out = new StringBuilder(raw.length());
		for (int i = 0; i < raw.length(); i++)
		{
			char c = raw.charAt(i);
			if (c != '\\')
			{
				out.append(c);
				continue;
			}
			if (i + 1 >= raw.length())
			{
				throw new RuntimeException("failed to parse " + raw);
			}
			char n = raw.charAt(++i);
			switch (n)
			{
				case '"':
				case '\\':
				case '/':
					out.append(n);
					break;
				case 'b':
					out.append('\b');
					break;
				case 'f':
					out.append('\f');
					break;
				case 'n':
					out.append('\n');
					break;
				case 'r':
					out.append('\r');
					break;
				case 't':
					out.append('\t');
					break;
				case 'u':
					if (i + 4 >= raw.length())
					{
						throw new RuntimeException("failed to parse " + raw);
					}
					String hex = raw.substring(i + 1, i + 5);
					try
					{
						out.append((char) Integer.parseInt(hex, 16));
					}
					catch (NumberFormatException bad)
					{
						throw new RuntimeException("failed to parse " + raw);
					}
					i += 4;
					break;
				default:
					throw new RuntimeException("failed to parse " + raw);
			}
		}
		return out.toString();
	}

	private static String escape(String raw)
	{
		StringBuilder out = new StringBuilder(raw.length() + 8);
		for (int i = 0; i < raw.length(); i++)
		{
			char c = raw.charAt(i);
			switch (c)
			{
				case '"':
					out.append("\\\"");
					break;
				case '\\':
					out.append("\\\\");
					break;
				case '\b':
					out.append("\\b");
					break;
				case '\f':
					out.append("\\f");
					break;
				case '\n':
					out.append("\\n");
					break;
				case '\r':
					out.append("\\r");
					break;
				case '\t':
					out.append("\\t");
					break;
				default:
					if (c < 0x20)
					{
						out.append("\\u00");
						String hex = Integer.toHexString(c);
						if (hex.length() == 1)
						{
							out.append('0');
						}
						out.append(hex);
					}
					else
					{
						out.append(c);
					}
			}
		}
		return out.toString();
	}

	private static Number number(Object value)
	{
		if (value instanceof Number)
		{
			return (Number) value;
		}
		if (value == null)
		{
			throw new NullPointerException();
		}
		throw new ClassCastException(value.getClass().getName());
	}

	private static BigDecimal bigDecimal(Object value)
	{
		if (value == null || value instanceof BigDecimal)
		{
			return (BigDecimal) value;
		}
		if (value instanceof Integer)
		{
			return new BigDecimal((Integer) value);
		}
		if (value instanceof Long)
		{
			return BigDecimal.valueOf(((Long) value).longValue());
		}
		if (value instanceof Number)
		{
			return new BigDecimal(((Number) value).toString());
		}
		throw new ClassCastException(value.getClass().getName());
	}
}
