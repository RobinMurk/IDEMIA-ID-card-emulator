package IDcard;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.framework.AID;

import org.globalplatform.GPSystem;

public class IDApplet extends Applet {
	
	//file pointers and constants
	private final static byte MF = 0x00;
	private final static byte DF = 0x01;
	private final static byte EF = 0x02;
	private static byte FILE_POINTER;
	private static byte FILE_NAME_FIRST;
	private static byte FILE_NAME_SECOND;

	//personal data
	private static final byte[] SURNAME = {(byte)0x4A, (byte)0xC3, (byte)0x95, (byte)0x45, (byte)0x4F, (byte)0x52, (byte)0x47};
	private static final byte[] FIRST_NAME = {(byte)0x4A, (byte)0x41, (byte)0x41, (byte)0x4B, (byte)0x2D, 
		(byte)0x4B, (byte)0x52, (byte)0x49, (byte)0x53, (byte)0x54, (byte)0x4A, (byte)0x41, (byte)0x4E};
	private static final byte[] SEX = {77}; //M
	private static final byte[] CITIZEN = {69, 83, 84};  //EST
	private static final byte[] BIRTH = {48, 56, 32, 48, 49, 32, 49, 57, 56, 48, 32, 69, 83, 84};
	private static final byte[] ID_CODE = {51, 56, 48, 48, 49, 48, 56, 53, 55, 49, 56};
	private static final byte[] DOC_NUMBER = {65, 83, 48, 48, 48, 48, 51, 52, 51};
	private static final byte[] EXPIRY_DATE = {49, 51, 32, 48, 56, 32, 50, 48, 50, 51};
	private static final byte[] ISSUANCE = {49, 51, 32, 48, 56, 32, 50, 48, 49, 56};
	private static final byte[] RESIDENCE_TYPE = {0};
	private static final byte[] NOTES1 = {0};
	private static final byte[] NOTES2 = {0};
	private static final byte[] NOTES3 = {0};
	private static final byte[] NOTES4 = {0};
	private static final byte[] NOTES5 = {0};

	//historical bytes
	private static final byte[] ATRHistBytes =new byte[] {
		(byte)0x00, (byte)0x12, (byte)0x23, (byte)0x3F, (byte)0x53, (byte)0x65, 
		(byte)0x49, (byte)0x44, (byte)0x0F, (byte)0x90, (byte)0x00, (byte)0xF1
	};

	private static final byte[] ATRoriginalFeitian =new byte[] {
		(byte)0x09, (byte)0x44, (byte)0x31, (byte)0x31, (byte)0x43,
		(byte)0x52, (byte)0x02, (byte)0x00, (byte)0x25, (byte)0xC3
	};

	private static final byte[] ATRoriginalEMV =new byte[] {
		(byte)0x53, (byte)0x43, (byte)0x45, (byte)0x36, (byte)0x30, (byte)0x2D, 
		(byte)0x43, (byte)0x44, (byte)0x30, (byte)0x38, (byte)0x31, (byte)0x2D, 
		(byte)0x6E, (byte)0x46
	};

	private static final byte[] aid = new byte[] {
		(byte)0xA0, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x77, (byte)0x01, (byte)0x08,
		(byte)0x00, (byte)0x07, (byte)0x00, (byte)0x00, (byte)0xFE, (byte)0x00, (byte)0x00,
		(byte)0x01, (byte)0x00
	};
	private final AID testAID;


	//logging info
	private static short LOGGER_ALLOCATED_SIZE;
	private static byte[] APDULog;
	private static short logDataSize;
	private static short startingPosPointer;
	private static short logDataLeft;


	//constructor
	private IDApplet() {
		LOGGER_ALLOCATED_SIZE = 0x0800;
		APDULog = new byte[LOGGER_ALLOCATED_SIZE];
		logDataSize = 0;
		startingPosPointer = 0;
		logDataLeft = 0;
		testAID = new AID(aid, (short)0, (byte)aid.length);
	}


	/**
	 * Method invoked once when installing the applet on the JavaCard
	 * @param ba
	 * @param offset
	 * @param len
	 */
	public static void install(byte[] ba, short offset, byte len) {
		(new IDApplet()).register();
	}

	
	//method is invoked when the applet is selected
	//the T protocol is logged
	public boolean select(){
		logByte(APDU.getProtocol());
		return true;
	}

	public void deselect(){
		logByte((byte)0xDE);
	}

	/**
	 * Main method invoked by the terminal that processes the incoming APDU
	 */
	public void process(APDU apdu) {   

		byte[] buf = apdu.getBuffer();
		
		//log header first
		if(buf[ISO7816.OFFSET_INS] != (byte)0xDD){
			logAPDU(buf, apdu, (short) 5, true);
		}

		//for checking main actions requested by terminal
		switch (buf[ISO7816.OFFSET_CLA]) {
			case (byte)0x00:
				switch (buf[ISO7816.OFFSET_INS]) {
					case (byte)0xA4:
						short len;
						len = buf[ISO7816.OFFSET_LC] == (byte)0x00 ? (short) 0 : apdu.setIncomingAndReceive();
						logAPDU(buf, apdu, len, false);
						selectFile(buf, apdu, len);
						return;
					case (byte)0xB0:
						readFile(buf, apdu);
						return;
					case (byte)0xDD:
						process_mock_commands(buf, apdu);
						return;
					default:
						logAPDU(buf, apdu, getLcValue(buf), false);
						ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
						return;
				}
		default:
			logAPDU(buf, apdu, getLcValue(buf), false);
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
			return;
		}
	}


	/**
	 * Processes mock commands to change internal settings and acces the logger
	 * @param buf APDU buffer
	 * @param apdu incoming APDU
	 */
	private void process_mock_commands(byte[] buf, APDU apdu){
		switch (buf[ISO7816.OFFSET_P1]){

			// Set historical bytes for estID IDEMIA 2018
			case (byte)0x01:
				try{
					// Set historical bytes for IDEMIA 2018 ID card
					if(buf[ISO7816.OFFSET_P2] == (byte)0x01){
						if(GPSystem.setATRHistBytes(ATRHistBytes, (short)0, (byte)ATRHistBytes.length)){
							return;
						}else{
							ISOException.throwIt(ISO7816.SW_UNKNOWN);
						}
					}
					// Set original bytes for feitian card
					else if(buf[ISO7816.OFFSET_P2] == (byte)0x02){
						if(GPSystem.setATRHistBytes(ATRoriginalFeitian, (short)0, (byte)ATRoriginalFeitian.length)){
							return;
						}else{
							ISOException.throwIt(ISO7816.SW_UNKNOWN);
						}
					}
					// set original bytes for G&D card
					else if(buf[ISO7816.OFFSET_P2] == (byte)0x03){
						if(GPSystem.setATRHistBytes(ATRoriginalEMV, (short)0, (byte)ATRoriginalEMV.length)){
							return;
						}else{
							ISOException.throwIt(ISO7816.SW_UNKNOWN);
						}
					}
					ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
				}catch(ISOException e){
					throw e;
				}catch(Exception e){
					ISOException.throwIt(ISO7816.SW_UNKNOWN);
				}
				break;
			//get logger data
			case (byte)0x02:
				switch (buf[ISO7816.OFFSET_P2]){
					case (byte)0x01:
						getLoggerData(apdu, buf);
						return;
					case (byte)0x02:
						if(resetLogger()){
							return;
						}else{
							ISOException.throwIt(ISO7816.SW_WARNING_STATE_UNCHANGED);
						}
						break;
					case (byte)0x03:
						getLoggerIndex(apdu, buf);
						break;
					default:
						ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
						break;
				}
				break;
			default:
				ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
				break;
			}
	}


	/**
	 * Processes the SELECT FILE command for MF, DF, EF files. Assumes that setIncomingAndReceive() has been called
	 * @param buf APDU buffer
	 * @param apdu the incoming APDU
	 */
	private void selectFile(byte[] buf, APDU apdu, short length){
		switch (buf[ISO7816.OFFSET_P1]) {
			case MF:
				FILE_POINTER = MF;
				return;
			case DF:
			case EF:
				if(getLcValue(buf) == (short)2){
					FILE_NAME_FIRST = buf[ISO7816.OFFSET_CDATA];
					FILE_NAME_SECOND = buf[ISO7816.OFFSET_CDATA + 1];
					FILE_POINTER = buf[ISO7816.OFFSET_P1];
					return;
				}else{
					ISOException.throwIt((short)0x6A87);
				}
				break;
			case (byte) 0x04:
				if (!testAID.partialEquals(buf, ISO7816.OFFSET_CDATA, (byte) length)) {
					ISOException.throwIt(ISO7816.SW_FILE_NOT_FOUND);
				}
				return;
			default:
				ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
				return;
		}
	}



	/**
	 * Processes the READ BINARY command for EF files.
	 * @param buf APDU buffer
	 * @param apdu the incoming APDU
	 */
	private void readFile(byte[] buf, APDU apdu){
		if (FILE_POINTER != EF && FILE_NAME_FIRST != (byte)0x50) ISOException.throwIt(ISO7816.SW_FILE_NOT_FOUND);
		switch (FILE_NAME_SECOND) {
			case (byte)0x01:
				sendPD(SURNAME, buf, apdu);
				return;
			case (byte)0x02:
				sendPD(FIRST_NAME, buf, apdu);
				return;
			case (byte)0x03:
				sendPD(SEX, buf, apdu);
				return;
			case (byte)0x04:
				sendPD(CITIZEN, buf, apdu);
				return;
			case (byte)0x05:
				sendPD(BIRTH, buf, apdu);
				return;
			case (byte)0x06:
				sendPD(ID_CODE, buf, apdu);
				return;
			case (byte)0x07:
				sendPD(DOC_NUMBER, buf, apdu);
				return;
			case (byte)0x08:
				sendPD(EXPIRY_DATE, buf, apdu);
				return;
			case (byte)0x09:
				sendPD(ISSUANCE, buf, apdu);
				return;
			case (byte)0x0a:
				sendPD(RESIDENCE_TYPE, buf, apdu);
				return;
			case (byte)0x0b:
				sendPD(NOTES1, buf, apdu);
				return;
			case (byte)0x0c:
				sendPD(NOTES2, buf, apdu);
				return;
			case (byte)0x0d:
				sendPD(NOTES3, buf, apdu);
				return;
			case (byte)0x0e:
				sendPD(NOTES4, buf, apdu);
				return;
			case (byte)0x0f:
				sendPD(NOTES5, buf, apdu);
				return;
			default:
				ISOException.throwIt(ISO7816.SW_FILE_NOT_FOUND);
		}
	}

	/**
	 * Gets the value of the Lc byte (ISO7816.OFFSET_LC). NB! This only works, if 
	 * buffer is not previously overwritten
	 * @param buf APDU buffer, 
	 * @return value of the Lc byte
	 */
	private short getLcValue(byte[] buf){
		return (short) (buf[ISO7816.OFFSET_LC] & (short)0xff);
	}
	

	/**
	 * Sends the given data back to the terminal. Automatically returns
	 * status wart "90 00". NB T=0 protocol only supports 255 bytes of data being sent
	 * @param DATA Data to be sent, cannot exceed the APDU buffer size
	 * @param buf APDU buffer
	 * @param apdu incoming APDU
	 */
	private void sendPD(byte[] DATA, byte[] buf, APDU apdu){
		if (APDU.getProtocol() == APDU.PROTOCOL_T0) {
			if(buf[ISO7816.OFFSET_LC] == (byte)0x00){
				//send back the expected length
				short sendValue = (short)(0x6C00 | ((short)DATA.length & 0xFF));
				ISOException.throwIt(sendValue);
			}else{
				short len = getLcValue(buf);
				Util.arrayCopyNonAtomic(DATA, (short)0, buf, (short)0, len);
				apdu.setOutgoingAndSend((short)0, len);
			}
		}
		else {
			Util.arrayCopyNonAtomic(DATA, (short)0, buf, (short)0, (short)DATA.length);
			apdu.setOutgoingAndSend((short)0, (short)DATA.length);
		}
	}


	/**
	 * Logs the given bytes to the logger
	 * @param buf bytes to be logged
	 * @return true if logged successfully
	 */
	private boolean logAPDU(byte[] buf, APDU apdu, short length, boolean is_header){

		//overflow check
		if ((short)(logDataSize + length + (short)1) > (short)APDULog.length) return false;
		try{

			APDULog[logDataSize] = (byte)length;
			logDataSize += (short)1;
			//short offset = is_header ? (short)0 : (short)ISO7816.OFFSET_CDATA;
			if (is_header){
				Util.arrayCopyNonAtomic(buf, (short)0, APDULog, logDataSize, length);
			}else{
				Util.arrayCopyNonAtomic(buf, (short)ISO7816.OFFSET_CDATA, APDULog, logDataSize, length);
			}

			logDataSize += length;
			logDataLeft = logDataSize;
			return true;
		}catch(Exception e){
			return false;
		}

	}


	/**
	 * Resets the loggers bytes to 0 and sets the index pointer to 0
	 * @return True if the Logger is reset successfully
	 */
	private boolean resetLogger(){
		try {
			JCSystem.beginTransaction();
			Util.arrayFillNonAtomic(APDULog, (short)0, logDataSize, (byte)0x00);
			logDataSize = (short)0;
			startingPosPointer = (short)0;
			logDataLeft = (short)0;
			JCSystem.commitTransaction();
			return true;
			
		} catch (Exception e) {
			JCSystem.abortTransaction();

			return false;
		}
	}


	/**
	 * Returns the full logger data. First call is sent with Le = 0x00 and following requests
	 * @param apdu
	 * @param buf
	 */
	private void getLoggerData(APDU apdu, byte[] buf){
		try{
			if (APDU.getProtocol() == APDU.PROTOCOL_T0) {

				short len;
				//if no data is left to return
				if(buf[ISO7816.OFFSET_LC] == (byte)0x00 && logDataLeft == (short)0){
					//reset pointers
					logDataLeft = logDataSize;
					startingPosPointer = (short)0;
					return;
				}
				//main case when data is left to be sent
				else if(buf[ISO7816.OFFSET_LC] != (byte)0x00 && logDataLeft != (short)0){

					len = getLcValue(buf);
					short start = startingPosPointer;

					apdu.setOutgoing();
					apdu.setOutgoingLength(len);
					apdu.sendBytesLong(APDULog, start, len);
					
					//adjust pointers
					startingPosPointer += len;
					logDataLeft = (short)(logDataLeft - len);
					
					short sendValue = (short)(0x6C00 | (logDataLeft & 0xFF));
					ISOException.throwIt(sendValue);
				}
				//when first command comes in with Le = 0x00
				else if(buf[ISO7816.OFFSET_LC] == (byte)0x00 && logDataLeft != (short)0){
					if(logDataLeft > (short)255){
						len = (short)255;
					}else{
						len = logDataLeft > (short)0 ? logDataLeft : (short)0;
					}
					short sendValue = (short)(0x6C00 | (len & 0xFF));
					ISOException.throwIt(sendValue);
				}
			//if T=1 or T=CL
			}else{
				apdu.setOutgoing();
				apdu.setOutgoingLength(logDataSize);
				apdu.sendBytesLong(APDULog, (short)0, logDataSize);
			}
		}catch(ISOException e){
			throw e;
		}catch(Exception e){
			ISOException.throwIt(ISO7816.SW_DATA_INVALID);
		}
	}


	/**
	 * Returns the current index of the logger
	 * @param apdu the incoming APDU
	 * @param buf APDU buffer
	 */
	private void getLoggerIndex(APDU apdu, byte[] buf){
		Util.setShort(buf, (short)0, logDataSize);
		apdu.setOutgoingAndSend((short)0, (short)2);
	}

	/**
	 * Logs the T protocol used by the terminal
	 * @param protocol protocol id: <br><br>
	 * 0x00 -> T=0 <br><br>
	 * 0x01 -> T=1 <br><br>
	 * 0xX1 -> T=CL where high nibble might vary<br><br>
	 */
	private void logByte(byte protocol){
		APDULog[logDataSize] = (byte) 1;
		logDataSize += (short)1;
		APDULog[logDataSize] = protocol;
		logDataSize += (short)1;
		logDataLeft = logDataSize;
	}
}

