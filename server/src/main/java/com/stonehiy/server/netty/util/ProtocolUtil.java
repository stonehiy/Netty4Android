package com.stonehiy.server.netty.util;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;


public class ProtocolUtil
{
	public static byte[] intToByteArray(int value)
	{
		byte[] b = new byte[4];
		for(int i = 0; i < 4; i++)
		{
			int offset = (b.length - 1 - i) * 8;
			b[i] = (byte)((value >>> offset) & 0xFF);
		}
		return b;
	}


	public static int byteArrayToInt(byte[] b)
	{
		return byteArrayToInt(b, 0);
	}


	public static int byteArrayToInt(byte[] b, int offset)
	{
		int value = 0;
		int len = Math.min(b.length - offset, 4);
		for(int i = 0; i < len; i++)
		{
			int shift = (len - 1 - i) * 8;
			value += (b[i + offset] & 0x000000FF) << shift;
		}
		return value;
	}


	public static byte[] stringToByteArray(String str, String charset)
	{
		if(str == null)
		{
			str = "";
		}
		try
		{
			byte[] strBytes = str.getBytes(charset);
			int len = strBytes.length;
			byte[] b = new byte[4 + len];
			System.arraycopy(intToByteArray(len), 0, b, 0, 4);
			System.arraycopy(strBytes, 0, b, 4, len);
			return b;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}


	public static String readString(ByteArrayInputStream in, String charset) throws IOException
	{
		byte[] lenBytes = new byte[4];
		in.read(lenBytes);
		int len = byteArrayToInt(lenBytes);
		byte[] valueBytes = new byte[len];
		in.read(valueBytes);
		return new String(valueBytes, charset).trim();
	}


	public static int readInt(ByteArrayInputStream in, String charset) throws IOException
	{
		byte[] valueBytes = new byte[4];
		in.read(valueBytes);
		return byteArrayToInt(valueBytes);
	}


	public static byte[] floatToByteArray(float value)
	{
		ByteBuffer bb = ByteBuffer.allocate(4);
		byte[] retBytes = new byte[4];
		FloatBuffer fb = bb.asFloatBuffer();
		fb.put(value);
		bb.get(retBytes);
		return retBytes;
	}


	public static float byteArrayToFloat(byte[] v)
	{
		ByteBuffer bb = ByteBuffer.wrap(v);
		FloatBuffer fb = bb.asFloatBuffer();
		return fb.get();
	}


	public static void main(String[] args)
	{
		byte[] bytes = floatToByteArray(5.0000f);
		for(int i = 0; i < bytes.length; i++)
		{
			System.out.print(bytes[i] + ",");
		}
		System.out.println("");
		System.out.println(byteArrayToFloat(bytes));
		bytes = new byte[] {64, 63, 0, 0};
		System.out.println(byteArrayToFloat(bytes));
	}

}
