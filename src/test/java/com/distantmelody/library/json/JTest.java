package com.distantmelody.library.json;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class JTest
{
	@Test
	void plainStringRoundTrips()
	{
		J j = J.fromString("\"hello\"");
		assertEquals("hello", j.getString());
		assertEquals("\"hello\"", j.toString());
		assertEquals("hello", J.fromString(j.toString()).getString());
	}

	@Test
	void emptyStringIsAString()
	{
		J j = J.fromString("\"\"");
		assertEquals("", j.getString());
		assertEquals("\"\"", j.toString());
		assertEquals(1, j.values().length);
	}

	@Test
	void wordNullIsNotJsonNull()
	{
		J word = J.fromString("\"null\"");
		assertEquals("null", word.getString());
		assertEquals("\"null\"", word.toString());

		J empty = J.fromString("null");
		assertNull(empty.get());
		assertEquals("null", empty.toString());
		assertEquals(1, empty.values().length);
	}

	@Test
	void booleansRoundTrip()
	{
		assertTrue(J.fromString("true").getBoolean());
		assertEquals("true", J.fromString("true").toString());
		assertFalse(J.fromString("false").getBoolean());
		assertEquals("false", J.fromString("false").toString());
		assertTrue(J.fromString(" true ").getBoolean());
	}

	@Test
	void escapedQuoteStaysInsideTheString()
	{
		J j = J.fromString("\"hello\\\" world\"");
		assertEquals("hello\" world", j.getString());
		assertEquals("\"hello\\\" world\"", j.toString());
	}

	@Test
	void escapedQuoteDoesNotLetWhitespaceStrippingLeaveTheString()
	{
		J j = J.fromString("{ \"msg\" : \"hello\\\" world\" }");
		assertEquals("hello\" world", j.getString("msg"));
		assertEquals("{\"msg\":\"hello\\\" world\"}", j.toString());
	}

	@Test
	void escapedBackslashBeforeAQuoteEndsTheString()
	{
		J j = J.fromString("[\"a\\\\\",\"b\"]");
		assertEquals("a\\", j.getString(0));
		assertEquals("b", j.getString(1));
		assertEquals("[\"a\\\\\",\"b\"]", j.toString());
	}

	@Test
	void oddBackslashesKeepTheQuoteInside()
	{
		J j = J.fromString("[\"a\\\"\",\"b\"]");
		assertEquals("a\"", j.getString(0));
		assertEquals("b", j.getString(1));
		assertEquals("[\"a\\\"\",\"b\"]", j.toString());
	}

	@Test
	void threeBackslashesBeforeAQuoteStayInTheString()
	{
		J j = J.fromString("[\"a\\\\\\\"b\"]");
		assertEquals("a\\\"b", j.getString());
		assertEquals("[\"a\\\\\\\"b\"]", j.toString());
	}

	@Test
	void controlsUnescapeAndEscape()
	{
		J j = J.fromString("\"\\b\\f\\n\\r\\t\"");
		assertEquals("\b\f\n\r\t", j.getString());
		assertEquals("\"\\b\\f\\n\\r\\t\"", j.toString());
	}

	@Test
	void rawControlsInAJavaStringAreEscapedOnOutput()
	{
		J j = J.M("k", "a\nb\tc");
		assertEquals("{\"k\":\"a\\nb\\tc\"}", j.toString());
		assertEquals("a\nb\tc", J.fromString(j.toString()).getString("k"));
	}

	@Test
	void otherControlCharactersUseAUnicodeEscape()
	{
		J j = J.A("\u0001");
		assertEquals("[\"\\u0001\"]", j.toString());
		assertEquals("\u0001", J.fromString(j.toString()).getString());
	}

	@Test
	void unicodeEscapeBecomesTheCharacter()
	{
		J j = J.fromString("\"\\u0041\\u00e9\"");
		assertEquals("A\u00e9", j.getString());
		assertEquals("\"A\u00e9\"", j.toString());
	}

	@Test
	void unicodeQuoteIsTextNotADelimiter()
	{
		J j = J.fromString("\"a\\u0022b\"");
		assertEquals("a\"b", j.getString());
		assertEquals("\"a\\\"b\"", j.toString());
	}

	@Test
	void surrogatePairRoundTrips()
	{
		J j = J.fromString("\"\\uD83D\\uDE00\"");
		assertEquals("\uD83D\uDE00", j.getString());
		assertEquals("\"\uD83D\uDE00\"", j.toString());
		assertEquals("\uD83D\uDE00", J.fromString(j.toString()).getString());
	}

	@Test
	void solidusIsUnescapedAndNotReescaped()
	{
		J j = J.fromString("\"http:\\/\\/example.com/a\"");
		assertEquals("http://example.com/a", j.getString());
		assertEquals("\"http://example.com/a\"", j.toString());
	}

	@Test
	void commaInsideAStringDoesNotSplit()
	{
		J j = J.fromString("[\"a,b\",\"c\"]");
		assertEquals(2, j.getArraySize());
		assertEquals("a,b", j.getString(0));
		assertEquals("c", j.getString(1));
		assertEquals("[\"a,b\",\"c\"]", j.toString());
	}

	@Test
	void colonInsideAStringDoesNotSplitAMap()
	{
		J j = J.fromString("{\"a:b\":\"c:d\"}");
		assertEquals("c:d", j.getString("a:b"));
		assertTrue(j.contains("a:b"));
		assertEquals("{\"a:b\":\"c:d\"}", j.toString());
	}

	@Test
	void escapedQuoteInsideAKey()
	{
		J j = J.fromString("{\"a\\\"b\":1}");
		assertEquals(1, j.getInt("a\"b"));
		assertEquals("{\"a\\\"b\":1}", j.toString());
		assertEquals(1, J.fromString(j.toString()).getInt("a\"b"));
	}

	@Test
	void commaInsideAKeyDoesNotSplitTheMap()
	{
		J j = J.fromString("{\"a,b\":1,\"c\":2}");
		assertEquals(1, j.getInt("a,b"));
		assertEquals(2, j.getInt("c"));
	}

	@Test
	void bracesAndBracketsInsideStringsAreText()
	{
		J j = J.fromString("{\"a\":\"x{y}[z]\",\"b\":1}");
		assertEquals("x{y}[z]", j.getString("a"));
		assertEquals(1, j.getInt("b"));

		J list = J.fromString("[\"}\",\"{\",\"]\",\"[\"]");
		assertEquals("}", list.getString(0));
		assertEquals("{", list.getString(1));
		assertEquals("]", list.getString(2));
		assertEquals("[", list.getString(3));
		assertEquals("[\"}\",\"{\",\"]\",\"[\"]", list.toString());
	}

	@Test
	void whitespaceOutsideStringsIsStrippedAndInsideIsKept()
	{
		J j = J.fromString(" \n\t{ \"name\" : \"a b\" , \"n\" : 2 } \r ");
		assertEquals("a b", j.getString("name"));
		assertEquals(2, j.getInt("n"));
	}

	@Test
	void singleValueAndOneElementArrayDifferOnlyInToString()
	{
		J one = J.fromString("\"hi\"");
		J array = J.fromString("[\"hi\"]");
		assertEquals("hi", one.getString());
		assertEquals("hi", array.getString());
		assertEquals("hi", array.get());
		assertEquals(1, one.values().length);
		assertEquals(1, array.values().length);
		assertEquals("\"hi\"", one.toString());
		assertEquals("[\"hi\"]", array.toString());
	}

	@Test
	void numbersUseTheSameSingleValueRule()
	{
		assertEquals(42, J.fromString("42").getInt());
		assertEquals("42", J.fromString("42").toString());
		assertEquals("[42]", J.fromString("[42]").toString());
		assertEquals(42, J.fromString("[42]").getInt());
		assertEquals(10, J.fromString("[10,20]").getInt());
		assertEquals(2, J.fromString("[10,20]").values().length);
		assertEquals(1, J.fromString("10").values().length);
	}

	@Test
	void emptyArrayAndEmptyObject()
	{
		assertEquals("[]", J.fromString("[]").toString());
		assertEquals(0, J.fromString("[]").getArraySize());
		assertEquals("{}", J.fromString("{}").toString());
		assertEquals(0, J.fromString("{}").getMapSize());
		assertEquals("[]", J.A().toString());
		assertEquals("{}", J.M().toString());
	}

	@Test
	void nestedObjectsAndArraysRoundTrip()
	{
		J j = J.fromString("{\"a\":{\"b\":[true,null,\"x\\\"y\",{\"k\":\"v\"}]},\"n\":[1,2]}");
		J b = j.get("a").get("b");
		assertTrue(b.getBoolean(0));
		assertNull(b.get(1));
		assertEquals("x\"y", b.getString(2));
		assertEquals("v", b.getJ(3).getString("k"));
		assertEquals(1, j.get("n").getInt(0));
		assertEquals(2, j.get("n").getInt(1));
		assertEquals("[true,null,\"x\\\"y\",{\"k\":\"v\"}]", b.toString());
		assertEquals("v", J.fromString(j.toString()).get("a").get("b").getJ(3).getString("k"));
	}

	@Test
	void aSingleObjectAndAnArrayOfObjectsShareValuesj()
	{
		J single = J.fromString("{\"rows\":{\"id\":1}}").get("rows");
		J many = J.fromString("{\"rows\":[{\"id\":1},{\"id\":2}]}").get("rows");

		assertEquals(1, single.valuesj().length);
		assertEquals(1, single.valuesj()[0].getInt("id"));
		assertEquals("{\"id\":1}", single.toString());
		assertEquals(1, ((J) single.get()).getInt("id"));

		assertEquals(2, many.valuesj().length);
		assertEquals(1, many.valuesj()[0].getInt("id"));
		assertEquals(2, many.valuesj()[1].getInt("id"));
		assertEquals(1, many.getJ(0).getInt("id"));
		assertEquals("[{\"id\":1},{\"id\":2}]", many.toString());
	}

	@Test
	void builtStringsAndKeysAreEscaped()
	{
		J j = J.M("a\"b", "c\\d");
		assertEquals("c\\d", j.getString("a\"b"));
		assertEquals("{\"a\\\"b\":\"c\\\\d\"}", j.toString());
		assertEquals("c\\d", J.fromString(j.toString()).getString("a\"b"));
	}

	@Test
	void explicitArrayStaysAnArray()
	{
		assertEquals("[\"a\\\"b\"]", J.A("a\"b").toString());
	}

	@Test
	void bytesAndStreamsUseUtf8() throws Exception
	{
		byte[] data = "{\"k\":\"A\\u00e9\"}".getBytes(StandardCharsets.UTF_8);
		assertEquals("A\u00e9", J.fromBytes(data).getString("k"));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		J.fromString("\"\\uD83D\\uDE00\"").write(out);
		assertEquals("\uD83D\uDE00", J.fromInputStream(new ByteArrayInputStream(out.toByteArray())).getString());
	}

	@Test
	void brokenEscapeFails()
	{
		assertThrows(RuntimeException.class, () -> J.fromString("\"\\q\""));
		assertThrows(RuntimeException.class, () -> J.fromString("\"\\u12\""));
		assertThrows(RuntimeException.class, () -> J.fromString("\"unterminated"));
	}

	@Test
	void numericGettersAcceptTheSameTypesOnAValue()
	{
		assertNumber(J.A(Integer.valueOf(7)), 7);
		assertNumber(J.A(Long.valueOf(7L)), 7);
		assertNumber(J.A(Float.valueOf(7f)), 7);
		assertNumber(J.A(Double.valueOf(7d)), 7);
		assertNumber(J.A(new BigDecimal("7")), 7);
	}

	@Test
	void numericGettersAcceptTheSameTypesByName()
	{
		assertNamedNumber(Integer.valueOf(4));
		assertNamedNumber(Long.valueOf(4L));
		assertNamedNumber(Float.valueOf(4f));
		assertNamedNumber(Double.valueOf(4d));
		assertNamedNumber(new BigDecimal("4"));
	}

	@Test
	void jsonNumbersUseEveryNumericGetter()
	{
		J j = J.fromString("{\"n\":7,\"list\":[7,8]}");
		assertEquals(7, j.getInt("n"));
		assertEquals(7L, j.getLong("n"));
		assertEquals(7f, j.getFloat("n"));
		assertEquals(0, new BigDecimal("7").compareTo(j.getBigDecimal("n")));
		assertEquals(Integer.valueOf(7), j.getInteger("n"));
		assertEquals(7, j.getInt("list", 0));
		assertEquals(8f, j.getFloat("list", 1));
		assertArrayEquals(new int[] {7, 8}, j.getIntArray("list"));
		assertArrayEquals(new float[] {7f, 8f}, j.getFloatArray("list"));
	}

	@Test
	void integerLongGetterDoesNotCastToLong()
	{
		J j = J.A(Integer.valueOf(9));
		assertEquals(9L, j.getLong(0));
		assertEquals(9, j.getInt());
		assertEquals(9f, j.getFloat(0));
		assertEquals(0, new BigDecimal("9").compareTo(j.getBigDecimal()));
		assertEquals(Integer.valueOf(9), j.getInteger());
		assertArrayEquals(new int[] {9}, j.getIntArray());
	}

	@Test
	void missingKeysKeepTheirOldResults()
	{
		J j = J.M();
		assertNull(j.getString("missing"));
		assertNull(j.getInteger("missing"));
		assertNull(j.getBigDecimal("missing"));
		assertNull(j.getBoolean("missing"));
		assertFalse(j.getBoolean("missing", false));
		assertThrows(NullPointerException.class, () -> j.getInt("missing"));
		assertThrows(NullPointerException.class, () -> j.getLong("missing"));
		assertThrows(NullPointerException.class, () -> j.getFloat("missing"));
	}

	@Test
	void missingKeyReturnsTheSuppliedDefault()
	{
		J j = J.M();
		assertEquals("fallback", j.getString("missing", "fallback"));
		assertEquals(Integer.valueOf(5), j.getInteger("missing", Integer.valueOf(5)));
		assertNull(j.getInteger("missing", null));
		assertEquals(5L, j.getLong("missing", 5L));
		assertEquals(5f, j.getFloat("missing", 5f));
		assertEquals(0, BigDecimal.ONE.compareTo(j.getBigDecimal("missing", BigDecimal.ONE)));
		assertFalse(j.getBoolean("missing", false));
		assertTrue(j.getBoolean("missing", true));
	}

	@Test
	void presentNullDoesNotUseTheDefault()
	{
		J j = J.fromString("{\"s\":null,\"b\":null,\"n\":null,\"d\":null}");
		assertTrue(j.contains("s"));
		assertNull(j.getString("s", "fallback"));
		assertNull(j.getBoolean("b", true));
		assertNull(j.getInteger("n", Integer.valueOf(5)));
		assertNull(j.getBigDecimal("d", BigDecimal.ONE));
		assertThrows(NullPointerException.class, () -> j.getLong("n", 5L));
		assertThrows(NullPointerException.class, () -> j.getFloat("n", 5f));
	}

	@Test
	void presentValuesIgnoreTheDefault()
	{
		J j = J.fromString("{\"s\":\"hi\",\"b\":true,\"bf\":false,\"n\":7,\"d\":2}");
		assertEquals("hi", j.getString("s", "fallback"));
		assertEquals("", J.fromString("{\"s\":\"\"}").getString("s", "fallback"));
		assertEquals(Boolean.TRUE, j.getBoolean("b", false));
		assertEquals(Boolean.FALSE, j.getBoolean("bf", true));
		assertEquals(Integer.valueOf(7), j.getInteger("n", Integer.valueOf(5)));
		assertEquals(7L, j.getLong("n", 5L));
		assertEquals(7f, j.getFloat("n", 5f));
		assertEquals(0, new BigDecimal("2").compareTo(j.getBigDecimal("d", BigDecimal.ONE)));
	}

	@Test
	void nullNumberElementStillThrowsOnAPrimitiveGetter()
	{
		J j = J.fromString("[null]");
		assertNull(j.get());
		assertNull(j.getInteger(0));
		assertNull(j.getBigDecimal(0));
		assertThrows(NullPointerException.class, () -> j.getInt(0));
		assertThrows(NullPointerException.class, () -> j.getLong(0));
	}

	private static void assertNumber(J j, int expected)
	{
		assertEquals(expected, j.getInt());
		assertEquals(expected, j.getInt(0));
		assertEquals(expected, j.getLong(0));
		assertEquals((float) expected, j.getFloat(0));
		assertEquals(0, new BigDecimal(expected).compareTo(j.getBigDecimal()));
		assertEquals(0, new BigDecimal(expected).compareTo(j.getBigDecimal(0)));
		assertEquals(Integer.valueOf(expected), j.getInteger());
		assertEquals(Integer.valueOf(expected), j.getInteger(0));
	}

	private static void assertNamedNumber(Object value)
	{
		J j = J.M("n", value, "list", J.A(value));
		assertEquals(4, j.getInt("n"));
		assertEquals(4L, j.getLong("n"));
		assertEquals(4f, j.getFloat("n"));
		assertEquals(0, new BigDecimal("4").compareTo(j.getBigDecimal("n")));
		assertEquals(Integer.valueOf(4), j.getInteger("n"));
		assertEquals(4, j.getInt("list", 0));
		assertEquals(4f, j.getFloat("list", 0));
		assertEquals(0, new BigDecimal("4").compareTo(j.getBigDecimal("list", 0)));
		assertArrayEquals(new int[] {4}, j.getIntArray("list"));
		assertArrayEquals(new float[] {4f}, j.getFloatArray("list"));
	}
}
