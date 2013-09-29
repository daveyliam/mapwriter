package mapwriter.region;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Nbt {

	// each NBT element has the structure:
	//  byte              tag_id
	//  short             name_length
	//  byte[name_length] name
	//
	// use int for tag ID's rather than byte so we can have -1 as
	// a 'null' or empty element
	//
	// tag ID's:
	public static final byte TAG_END        = 0x00;
	public static final byte TAG_BYTE       = 0x01;
	public static final byte TAG_SHORT      = 0x02;
	public static final byte TAG_INT        = 0x03;
	public static final byte TAG_LONG       = 0x04;
	public static final byte TAG_FLOAT      = 0x05;
	public static final byte TAG_DOUBLE     = 0x06;
	public static final byte TAG_BYTE_ARRAY = 0x07;		// INT length, then length BYTE's
	public static final byte TAG_STRING     = 0x08;		// SHORT length, UTF-8 string
	public static final byte TAG_LIST       = 0x09;		// BYTE entry id, INT entry count, entries (without tag or name)
	public static final byte TAG_COMPOUND   = 0x0a;		// fully formed tags, ending at TAG_END
	public static final byte TAG_INT_ARRAY  = 0x0b;		// INT length, length INT's
	
	public static final byte TAG_NULL       = -1;
	
	public static Nbt nullElement = new Nbt(TAG_NULL, "", null);
	
	public byte tagID;
	public String name;
	private Object data;
	
	public Nbt(byte tagID, String name, Object data) {
		this.tagID = tagID;
		this.name = name;
		this.data = data;
	}
	
	public boolean isNull() {
		return (this.tagID == TAG_NULL);
	}
	
	public void addChild(Nbt child) {
		// for list structures
		if (this.tagID == TAG_LIST) {
			if (this.data == null) {
				this.data = new ArrayList<Nbt>();
			}
			@SuppressWarnings("unchecked")
			List<Nbt> childrenList = (List<Nbt>) this.data;
			childrenList.add(child);
		}
		
		// for compound structures
		if (this.tagID == TAG_COMPOUND) {
			if (this.data == null) {
				this.data = new HashMap<String, Nbt>();
			}
			@SuppressWarnings("unchecked")
			Map<String, Nbt> childrenMap = (Map<String, Nbt>) this.data;
			childrenMap.put(child.name, child);
		}
	}
	
	// getChild methods allow for invocation chaining.
	// e.g. int child10Data = elem.getChildOrEmpty(10).getInt()
	
	// for lists, get the child element at index 'index'
	public Nbt getChild(int index) {
		Nbt child = null;
		if ((this.tagID == TAG_LIST) && (this.data != null)) {
			@SuppressWarnings("unchecked")
			List<Nbt> childrenList = (List<Nbt>) this.data;
			if ((index >= 0) && (index < childrenList.size())) {
				child = childrenList.get(index);
			}
		}
		return (child != null) ? (child) : (nullElement);
	}
	
	// for compound tags, returns the child with name 'name'
	public Nbt getChild(String name) {
		Nbt child = null;
		if ((this.tagID == TAG_COMPOUND) && (this.data != null)) {
			@SuppressWarnings("unchecked")
			Map<String, Nbt> childrenMap = (Map<String, Nbt>) this.data;
			child = childrenMap.get(name);
		}
		return (child != null) ? (child) : (nullElement);
	}
	
	// for lists, get number of children
	public int size() {
		int size = 0;
		if ((this.tagID == TAG_LIST) && (this.data != null)) {
			@SuppressWarnings("unchecked")
			List<Nbt> childrenList = (List<Nbt>) this.data;
			size = childrenList.size();
		}
		return size;
	}
	
	/*public static String readString(DataInputStream dis) throws IOException {
		int stringLength = (int) dis.readShort();
		byte[] stringBytes = null;
		String s = "";
		if (stringLength > 0) {
			stringBytes = new byte[stringLength];
			s = dis.readUTF();
			try {
				s = new String(stringBytes, "UTF8");
			} catch (Exception e) {
				e.printStackTrace();
				s = "";
			}
		}
		return s;
	}
	
	public static void writeString(DataOutputStream dos, String s) throws IOException {
		dos.writeShort((short) s.length());
		byte[] stringBytes = null;
		try {
			dos.write(s.getBytes("UTF8"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/
	
	public static Nbt readNextElement(DataInputStream dis) throws IOException {
		byte tagID;
		String name = "";
		
		// fully formed tag
		tagID = (byte) dis.readByte();
		if (tagID != TAG_END) {
			// END tags do not have a name
			name = dis.readUTF();
		}
		
		return readElementData(dis, tagID, name);
	}
	
	// parse the next nbt tag in the data buffer
	public static Nbt readElementData(DataInputStream dis, byte tagID, String name) throws IOException{
		Nbt elem = null;
		switch (tagID) {
			case TAG_END:
				//System.out.format("encountered end tag at %d\n", buffer.position());
				break;
			case TAG_BYTE:
				elem = new Nbt(tagID, name, new Byte(dis.readByte()));
				break;
			case TAG_SHORT:
				elem = new Nbt(tagID, name, new Short(dis.readShort()));
				break;
			case TAG_INT:
				elem = new Nbt(tagID, name, new Integer(dis.readInt()));
				break;
			case TAG_FLOAT:
				elem = new Nbt(tagID, name, new Float(dis.readFloat()));
				break;
			case TAG_LONG:
				elem = new Nbt(tagID, name, new Long(dis.readLong()));
				break;
			case TAG_DOUBLE:
				elem = new Nbt(tagID, name, new Double(dis.readDouble()));
				break;
				
			case TAG_BYTE_ARRAY:
				int baLength = dis.readInt();
				byte[] byteArray = null;
				if (baLength > 0) {
					byteArray = new byte[baLength];
					dis.readFully(byteArray);
				}
				elem = new Nbt(tagID, name, byteArray);
				break;
				
			case TAG_INT_ARRAY:
				int iaLength = dis.readInt();
				int[] intArray = null;
				if (iaLength > 0) {
					intArray = new int[iaLength];
					for (int i = 0; i < iaLength; i++) {
						intArray[i] = dis.readInt();
					}
				}
				elem = new Nbt(tagID, name, intArray);
				break;
				
			case TAG_STRING:
				elem = new Nbt(tagID, name, dis.readUTF());
				break;
				
			case TAG_LIST:
				byte childType = dis.readByte();
				int listLength = dis.readInt();
				elem = new Nbt(tagID, name, null);
				//System.out.format("encountered list tag at %d, childType %d, length %d\n", buffer.position(), childType, listLength);
				for (int i = 0; i < listLength; i++) {
					Nbt child = readElementData(dis, childType, "");
					elem.addChild(child);
				}
				break;
				
			case TAG_COMPOUND:
				elem = new Nbt(tagID, name, null);
				boolean end = false;
				while (!end) {
					Nbt child = readNextElement(dis);
					if (child.isNull()) {
						end = true;
					} else {
						elem.addChild(child);
					}
				}
				break;
			
			default:
				System.out.format("error: encountered unknown tag id\n");
				break;
		}
		
		return (elem != null) ? elem : nullElement;
	}
	
	public byte getByte() {
		return ((this.tagID == TAG_BYTE) && (this.data != null)) ? ((Byte) this.data) : ((byte) 0);
	}
	
	public short getShort() {
		return ((this.tagID == TAG_SHORT) && (this.data != null)) ? ((Short) this.data) : ((short) 0);
	}
	
	public int getInt() {
		return ((this.tagID == TAG_INT) && (this.data != null)) ? ((Integer) this.data) : ((int) 0);
	}
	
	public long getLong() {
		return ((this.tagID == TAG_LONG) && (this.data != null)) ? ((Long) this.data) : ((long) 0);
	}
	
	public float getFloat() {
		return ((this.tagID == TAG_FLOAT) && (this.data != null)) ? ((Float) this.data) : ((float) 0);
	}
	
	public double getDouble() {
		return ((this.tagID == TAG_DOUBLE) && (this.data != null)) ? ((Double) this.data) : ((double) 0);
	}
	
	public byte[] getByteArray() {
		return ((this.tagID == TAG_BYTE_ARRAY) && (this.data != null)) ? ((byte[]) this.data) : (null);
	}
	
	public int[] getIntArray() {
		return ((this.tagID == TAG_INT_ARRAY) && (this.data != null)) ? ((int[]) this.data) : (null);
	}
	
	public String getString() {
		return ((this.tagID == TAG_STRING) && (this.data != null)) ? ((String) this.data) : (null);
	}
	
	
	public void writeElement(DataOutputStream dos) throws IOException{
		dos.writeByte(this.tagID);
		if (this.tagID != TAG_END) {
			dos.writeUTF(this.name);
			this.writeElementData(dos);
		}
	}
	
	public void writeElementData(DataOutputStream dos) throws IOException {
		//Mw.log("Nbt.writeElementData: %02x '%s'", this.tagID, this.name);
		switch (this.tagID) {
		case TAG_END:
			break;
		case TAG_BYTE:
			dos.writeByte(this.getByte());
			break;
		case TAG_SHORT:
			dos.writeShort(this.getShort());
			break;
		case TAG_INT:
			dos.writeInt(this.getInt());
			break;
		case TAG_FLOAT:
			dos.writeFloat(this.getFloat());
			break;
		case TAG_LONG:
			dos.writeLong(this.getLong());
			break;
		case TAG_DOUBLE:
			dos.writeDouble(this.getDouble());
			break;
			
		case TAG_BYTE_ARRAY:
			byte[] byteArray = this.getByteArray();
			if (byteArray != null) {
				dos.writeInt(byteArray.length);
				dos.write(byteArray);
			} else {
				dos.writeInt(0);
			}
			break;
			
		case TAG_INT_ARRAY:
			int[] intArray = this.getIntArray();
			if (intArray != null) {
				dos.writeInt(intArray.length);
				for (int i = 0; i < intArray.length; i++) {
					dos.writeInt(intArray[i]);
				}
			} else {
				dos.writeInt(0);
			}
			break;
			
		case TAG_STRING:
			dos.writeUTF(this.getString());
			break;
			
		case TAG_LIST:
			int listLength = this.size();
			if (this.size() > 0) {
				dos.writeByte(this.getChild(0).tagID);
				dos.writeInt(listLength);
				for (int i = 0; i < listLength; i++) {
					this.getChild(i).writeElementData(dos);
				}
			} else {
				dos.writeByte(TAG_BYTE);
				dos.writeInt(0);
			}
			break;
			
		case TAG_COMPOUND:
			@SuppressWarnings("unchecked")
			Map<String, Nbt> childrenMap = (Map<String, Nbt>) this.data;
			for (Nbt child : childrenMap.values()) {
				if (child != null) {
					child.writeElement(dos);
				}
			}
			dos.writeByte(TAG_END);
			break;
		
		default:
			System.out.format("error: encountered unknown tag id\n");
			break;
		}
	}
}