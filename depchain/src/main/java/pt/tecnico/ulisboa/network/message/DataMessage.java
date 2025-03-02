package pt.tecnico.ulisboa.network.message;


import java.io.*;

public class DataMessage implements Message, Serializable {
	private static final long serialVersionUID = 1L;
	public static final byte TYPE_INDICATOR = 1;
	private static final byte[] EOF_INDICATOR = "EOF".getBytes();
	
	private String id;
	private final String destination;
	private final int port;
	private final byte[] content;
	private final long seqNum;
	
	private long sendCooldown = 1;
	private long sendCounter = 1;
	
	public DataMessage(String destination, int port, byte[] content, long seqNum) {
		this.destination = destination;
		this.port = port;
		this.content = content;
		this.seqNum = seqNum;
	}
	
	@Override
	public byte getType() {
		return TYPE_INDICATOR;
	}
	
	public String getDestination() {
		return destination;
	}
	
	public int getPort() {
		return port;
	}
	
	public byte[] getContent() {
		return content;
	}
	
	@Override
	public long getSeqNum() {
		return seqNum;
	}
	
	@Override
	public String getId() {
		return id;
	}
	
	public long getCounter() {
		return sendCounter;
	}
	
	public long getCooldown() {
		return sendCooldown;
	}
	
	public void setKey(String key) {
		this.id = key;
	}
	
	public void setCounter(long sendCounter) {
		this.sendCounter = sendCounter;
	}
	
	public void setCooldown(long sendCooldown) {
		this.sendCooldown = sendCooldown;
	}
	
	public void incrementCounter() {
		sendCounter++;
	}
	
	public void doubleCooldown() {
		sendCooldown = Math.min(sendCooldown * 2, 10000000);
	}
	
	@Override
	public byte[] serialize() {
		try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
			byteStream.write(TYPE_INDICATOR);
			
			try (DataOutputStream dataStream = new DataOutputStream(byteStream)) {
				dataStream.writeUTF(destination);
				dataStream.writeInt(port);
				dataStream.writeLong(seqNum);
				dataStream.writeInt(content.length);
				dataStream.write(content);
				
				dataStream.write(EOF_INDICATOR);
			}
			
			return byteStream.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static DataMessage fromByteArray(byte[] data) {
		try (ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
			 DataInputStream dataStream = new DataInputStream(byteStream)) {
			
			if (dataStream.available() < 16 + EOF_INDICATOR.length) {
				System.err.println("Error: Insufficient data for DataMessage deserialization.");
				return null;
			}
			
			String destination = dataStream.readUTF();
			int port = dataStream.readInt();
			long seqNum = dataStream.readLong();
			int length = dataStream.readInt();
			
			if (length < 0 || length > dataStream.available() - EOF_INDICATOR.length) {
				System.err.println("Error: Invalid content length.");
				return null;
			}
			
			byte[] content = new byte[length];
			dataStream.readFully(content);
			
			byte[] eofCheck = new byte[EOF_INDICATOR.length];
			dataStream.readFully(eofCheck);
			if (!java.util.Arrays.equals(eofCheck, EOF_INDICATOR)) {
				System.err.println("Error: EOF indicator not found.");
				return null;
			}
			
			return new DataMessage(destination, port, content, seqNum);
			
		} catch (EOFException e) {
			System.err.println("Error: Unexpected end of file during DataMessage deserialization.");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Error during DataMessage deserialization: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	public void printMessage() {
		System.out.println("DataMessage: Destination=" + destination + ", Port=" + port + ", Sequence Number=" + seqNum + ", Content Length=" + (content != null ? content.length : 0) + " bytes, Type=" + TYPE_INDICATOR);
	}
}